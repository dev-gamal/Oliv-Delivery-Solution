package ma.skaii.oliv.service;

import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.entity.ProfilLivreur;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.domain.enums.StatutCommande;
import ma.skaii.oliv.domain.enums.StatutVerificationLivreur;
import ma.skaii.oliv.repository.CommandeRepository;
import ma.skaii.oliv.repository.ProfilLivreurRepository;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LivreurService {

    private final ProfilLivreurRepository profilLivreurRepository;
    private final CommandeRepository commandeRepository;
    private final AuditService auditService;

    private static final Set<StatutCommande> STATUTS_COURSE_ACTIVE = EnumSet.of(
            StatutCommande.ACCEPTEE,
            StatutCommande.EN_ROUTE_COLLECTE,
            StatutCommande.COLLECTEE,
            StatutCommande.EN_LIVRAISON
    );

    @Transactional
    public ProfilLivreur basculerDisponibilite(Utilisateur livreurUser, boolean passerEnLigne) {
        ProfilLivreur profil = profilLivreurRepository.findByUtilisateurId(livreurUser.getId())
                .orElseThrow(() -> new BusinessRuleException("Profil livreur introuvable."));

        if (passerEnLigne && profil.getStatutVerification() != StatutVerificationLivreur.VERIFIE) {
            throw new BusinessRuleException("Section 3 : Votre dossier livreur doit avoir le statut VERIFIE avant de passer en ligne.");
        }

        profil.setEnLigne(passerEnLigne);
        return profilLivreurRepository.save(profil);
    }

    /**
     * Section 7.4 : Géolocalisation du livreur collectée uniquement pendant une course active.
     */
    @Transactional
    public ProfilLivreur actualiserPositionGpsPendantCourse(Utilisateur livreurUser, double lat, double lng) {
        ProfilLivreur profil = profilLivreurRepository.findByUtilisateurId(livreurUser.getId())
                .orElseThrow(() -> new BusinessRuleException("Profil livreur introuvable."));

        List<Commande> coursesLivreur = commandeRepository.findByLivreurAffecteIdOrderByDateCreationDesc(profil.getId());
        boolean aUneCourseEnCours = coursesLivreur.stream()
                .anyMatch(c -> STATUTS_COURSE_ACTIVE.contains(c.getStatut()));

        if (!aUneCourseEnCours) {
            throw new BusinessRuleException("Conformité CNDP (Section 7.4) : La position GPS n'est collectée que pendant une course active.");
        }

        profil.setDerniereLatitude(lat);
        profil.setDerniereLongitude(lng);
        profil.setDateDernierePosition(OffsetDateTime.now());
        return profilLivreurRepository.save(profil);
    }

    @Transactional
    public ProfilLivreur validerOnboardingLivreur(UUID profilLivreurId, Utilisateur admin, String motif, String ip) {
        ProfilLivreur profil = profilLivreurRepository.findById(profilLivreurId)
                .orElseThrow(() -> new BusinessRuleException("Profil livreur introuvable."));

        if (!profil.isFormationCourteValidee() || profil.getPieceIdentiteUrl() == null) {
            throw new BusinessRuleException("Les pièces d'identité et la validation de la formation courte sont obligatoires.");
        }

        String ancienStatut = profil.getStatutVerification().name();
        profil.setStatutVerification(StatutVerificationLivreur.VERIFIE);

        auditService.enregistrerActionSensible(
                admin, "VALIDATION_KYC_LIVREUR", "ProfilLivreur",
                profil.getId().toString(), ancienStatut, "VERIFIE", motif, ip
        );

        return profilLivreurRepository.save(profil);
    }
}