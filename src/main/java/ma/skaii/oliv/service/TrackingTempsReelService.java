package ma.skaii.oliv.service;

import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.entity.ProfilLivreur;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.domain.enums.StatutCommande;
import ma.skaii.oliv.dto.livreur.PositionGpsRequest;
import ma.skaii.oliv.dto.tracking.TrackingPositionEvent;
import ma.skaii.oliv.repository.CommandeRepository;
import ma.skaii.oliv.repository.ProfilLivreurRepository;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrackingTempsReelService {

    private final ProfilLivreurRepository profilLivreurRepository;
    private final CommandeRepository commandeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final Set<StatutCommande> STATUTS_COURSE_ACTIVE = EnumSet.of(
            StatutCommande.ACCEPTEE,
            StatutCommande.EN_ROUTE_COLLECTE,
            StatutCommande.COLLECTEE,
            StatutCommande.EN_LIVRAISON
    );

    /**
     * Reçoit la position du livreur, la sauvegarde et la diffuse en temps réel via WebSocket.
     */
    @Transactional
    public TrackingPositionEvent traiterEtDiffuserPosition(Utilisateur livreurUser, PositionGpsRequest request) {
        ProfilLivreur profil = profilLivreurRepository.findByUtilisateurId(livreurUser.getId())
                .orElseThrow(() -> new BusinessRuleException("Profil livreur introuvable."));

        // Vérifier que le livreur a bien une course active (Conformité CNDP - Section 7.4)
        List<Commande> coursesActives = commandeRepository
                .findByLivreurAffecteIdOrderByDateCreationDesc(profil.getId())
                .stream()
                .filter(c -> STATUTS_COURSE_ACTIVE.contains(c.getStatut()))
                .toList();

        if (coursesActives.isEmpty()) {
            throw new BusinessRuleException(
                    "Conformité CNDP (Section 7.4) : la position GPS n'est collectée que pendant une course active.");
        }

        OffsetDateTime maintenant = OffsetDateTime.now();
        profil.setDerniereLatitude(request.latitude());
        profil.setDerniereLongitude(request.longitude());
        profil.setDateDernierePosition(maintenant);
        profilLivreurRepository.save(profil);

        TrackingPositionEvent dernierEvent = null;

        for (Commande commande : coursesActives) {
            TrackingPositionEvent event = new TrackingPositionEvent(
                    commande.getReferenceUnique(),
                    commande.getStatut(),
                    request.latitude(),
                    request.longitude(),
                    request.capDegres(),
                    request.vitesseKmh(),
                    maintenant
            );

            // Diffusion en temps réel vers le canal privé de la commande (Expéditeur + Destinataire)
            messagingTemplate.convertAndSend(
                    "/topic/suivi/" + commande.getTokenSuiviDestinataire(),
                    event
            );

            // Diffusion vers la carte opérationnelle du back-office Dispatch (Section 7.5)
            messagingTemplate.convertAndSend(
                    "/topic/dispatch/zone/" + commande.getZone(),
                    event
            );

            dernierEvent = event;
        }

        return dernierEvent;
    }

    /**
     * Endpoint REST léger pour récupérer la dernière position connue (chargement initial de la carte).
     */
    @Transactional(readOnly = true)
    public TrackingPositionEvent obtenirDernierePositionParToken(String tokenSuivi) {
        Commande commande = commandeRepository.findByTokenSuiviDestinataire(tokenSuivi)
                .orElseThrow(() -> new BusinessRuleException("Lien de suivi invalide ou introuvable."));

        ProfilLivreur livreur = commande.getLivreurAffecte();
        boolean positionVisible = livreur != null && STATUTS_COURSE_ACTIVE.contains(commande.getStatut());

        return new TrackingPositionEvent(
                commande.getReferenceUnique(),
                commande.getStatut(),
                positionVisible ? livreur.getDerniereLatitude() : null,
                positionVisible ? livreur.getDerniereLongitude() : null,
                null,
                null,
                positionVisible ? livreur.getDateDernierePosition() : null
        );
    }
}