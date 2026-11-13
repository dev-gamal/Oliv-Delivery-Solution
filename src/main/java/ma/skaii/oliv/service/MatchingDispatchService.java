package ma.skaii.oliv.service;

import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.entity.OffreCourse;
import ma.skaii.oliv.domain.entity.ProfilLivreur;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.domain.enums.*;
import ma.skaii.oliv.repository.CommandeRepository;
import ma.skaii.oliv.repository.OffreCourseRepository;
import ma.skaii.oliv.repository.ProfilLivreurRepository;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingDispatchService {

    private final ProfilLivreurRepository profilLivreurRepository;
    private final OffreCourseRepository offreCourseRepository;
    private final CommandeRepository commandeRepository;
    private final CommandeStateMachineService stateMachineService;

    @Value("${oliv.dispatch.default-offer-timeout-seconds:60}")
    private int delaiReponseParDefautSecondes;

    /**
     * Étape 4 : Identifie et classe les livreurs candidats (Zone, En ligne, KYC VERIFIE, Véhicule, Distance approche).
     */
    @Transactional(readOnly = true)
    public List<ProfilLivreur> rechercherLivreursCandidatsClasses(UUID commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new BusinessRuleException("Commande introuvable."));

        // Filtre strict en base : enLigne = true ET statutVerification = VERIFIE (Section 3 & 16)
        List<ProfilLivreur> candidats = profilLivreurRepository
                .findByEnLigneTrueAndStatutVerificationAndZoneAffectationAndVehiculeDeclare(
                        StatutVerificationLivreur.VERIFIE,
                        commande.getZone(),
                        commande.getVehiculeRequis()
                );

        // Classement par distance au point de collecte (ascendant), puis par note moyenne (descendant)
        return candidats.stream()
                .filter(l -> l.getUtilisateur().getStatut() == StatutCompte.ACTIF)
                .sorted(Comparator
                        .comparingDouble((ProfilLivreur l) -> calculerDistanceKmHaversine(
                                commande.getLatitudeCollecte(), commande.getLongitudeCollecte(),
                                l.getDerniereLatitude(), l.getDerniereLongitude()))
                        .thenComparing(ProfilLivreur::getNoteMoyenne, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Étape 5 (Envoi) : L'opérateur envoie une offre temporisée à un livreur candidat.
     */
    @Transactional
    public OffreCourse envoyerOffreManuelle(UUID commandeId, UUID profilLivreurId, Utilisateur operateur) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new BusinessRuleException("Commande introuvable."));

        ProfilLivreur livreur = profilLivreurRepository.findById(profilLivreurId)
                .orElseThrow(() -> new BusinessRuleException("Livreur introuvable."));

        // Vérifications de sécurité (Section 3 & 7.2)
        if (livreur.getStatutVerification() != StatutVerificationLivreur.VERIFIE
                || livreur.getUtilisateur().getStatut() != StatutCompte.ACTIF) {
            throw new BusinessRuleException("Impossible d'envoyer une offre à un livreur non vérifié ou suspendu.");
        }
        if (livreur.getVehiculeDeclare() != commande.getVehiculeRequis()) {
            throw new BusinessRuleException("Le véhicule du livreur ne correspond pas au véhicule requis pour la course.");
        }

        double distApproche = calculerDistanceKmHaversine(
                commande.getLatitudeCollecte(), commande.getLongitudeCollecte(),
                livreur.getDerniereLatitude(), livreur.getDerniereLongitude()
        );

        OffsetDateTime maintenant = OffsetDateTime.now();
        OffreCourse offre = OffreCourse.builder()
                .commande(commande)
                .livreur(livreur)
                .modeDispatch(ModeDispatch.MANUEL_ASSISTE)
                .operateurDispatch(operateur)
                .distanceApprocheCollecteKm(BigDecimal.valueOf(distApproche).setScale(2, RoundingMode.HALF_UP))
                .gainNetEstimeAfficheDh(commande.getDetailFinancier().getGainNetLivreurDh())
                .heureEnvoi(maintenant)
                .delaiReponseSecondes(delaiReponseParDefautSecondes)
                .heureExpiration(maintenant.plusSeconds(delaiReponseParDefautSecondes))
                .statutReponse(StatutOffre.ENVOYEE)
                .build();

        offreCourseRepository.save(offre);
        stateMachineService.changerStatut(commande, StatutCommande.OFFRE_ENVOYEE, operateur,
                "Offre envoyée au livreur " + livreur.getUtilisateur().getNomComplet());
        return offre;
    }

    /**
     * Étape 5 (Réponse) : Le livreur accepte ou refuse l'offre dans le délai imparti.
     */
    @Transactional
    public OffreCourse repondreAOffre(UUID offreId, Utilisateur livreurUser, boolean acceptee, String motifRefus) {
        OffreCourse offre = offreCourseRepository.findById(offreId)
                .orElseThrow(() -> new BusinessRuleException("Offre de course introuvable."));

        if (!offre.getLivreur().getUtilisateur().getId().equals(livreurUser.getId())) {
            throw new BusinessRuleException("Règle n°8 : Vous ne pouvez répondre qu'aux offres qui vous sont destinées.");
        }
        if (offre.getStatutReponse() != StatutOffre.ENVOYEE) {
            throw new BusinessRuleException("Cette offre a déjà été traitée ou a expiré.");
        }

        Commande commande = offre.getCommande();
        OffsetDateTime maintenant = OffsetDateTime.now();

        // Gestion de l'expiration du délai
        if (maintenant.isAfter(offre.getHeureExpiration())) {
            offre.setStatutReponse(StatutOffre.EXPIREE);
            offreCourseRepository.save(offre);
            stateMachineService.changerStatut(commande, StatutCommande.EN_RECHERCHE_LIVREUR, livreurUser, "Délai d'offre expiré");
            throw new BusinessRuleException("Le délai de réponse à cette offre est expiré.");
        }

        offre.setHeureReponse(maintenant);

        if (acceptee) {
            offre.setStatutReponse(StatutOffre.ACCEPTEE);
            commande.setLivreurAffecte(offre.getLivreur());
            stateMachineService.changerStatut(commande, StatutCommande.ACCEPTEE, livreurUser, "Offre acceptée par le livreur");
        } else {
            offre.setStatutReponse(StatutOffre.REFUSEE);
            offre.setMotifRefus(motifRefus);
            stateMachineService.changerStatut(commande, StatutCommande.EN_RECHERCHE_LIVREUR, livreurUser,
                    "Offre refusée par le livreur : " + (motifRefus != null ? motifRefus : "Sans motif"));
        }

        return offreCourseRepository.save(offre);
    }

    private double calculerDistanceKmHaversine(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return 999.0;
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}