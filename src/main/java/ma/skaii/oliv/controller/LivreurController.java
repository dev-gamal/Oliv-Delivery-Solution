package ma.skaii.oliv.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.entity.IncidentLitige;
import ma.skaii.oliv.domain.entity.OffreCourse;
import ma.skaii.oliv.domain.entity.ProfilLivreur;
import ma.skaii.oliv.domain.enums.StatutOffre;
import ma.skaii.oliv.dto.commande.CommandeExpediteurResponse;
import ma.skaii.oliv.dto.dispatch.OffreCourseLivreurResponse;
import ma.skaii.oliv.dto.dispatch.ReponseOffreRequest;
import ma.skaii.oliv.dto.incident.IncidentCreateRequest;
import ma.skaii.oliv.dto.incident.IncidentResponse;
import ma.skaii.oliv.dto.livreur.DisponibiliteRequest;
import ma.skaii.oliv.dto.livreur.PositionGpsRequest;
import ma.skaii.oliv.dto.livreur.ProfilLivreurResponse;
import ma.skaii.oliv.dto.preuve.PreuveSubmitRequest;
import ma.skaii.oliv.mapper.CommandeMapper;
import ma.skaii.oliv.mapper.DispatchMapper;
import ma.skaii.oliv.mapper.LivreurMapper;
import ma.skaii.oliv.mapper.PreuveIncidentMapper;
import ma.skaii.oliv.repository.OffreCourseRepository;
import ma.skaii.oliv.repository.ProfilLivreurRepository;
import ma.skaii.oliv.security.CustomUserDetails;
import ma.skaii.oliv.service.CommandeService;
import ma.skaii.oliv.service.LivreurService;
import ma.skaii.oliv.service.MatchingDispatchService;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/livreur")
@RequiredArgsConstructor
public class LivreurController {

    private final LivreurService livreurService;
    private final MatchingDispatchService matchingDispatchService;
    private final CommandeService commandeService;
    private final ProfilLivreurRepository profilLivreurRepository;
    private final OffreCourseRepository offreCourseRepository;
    private final LivreurMapper livreurMapper;
    private final DispatchMapper dispatchMapper;
    private final CommandeMapper commandeMapper;
    private final PreuveIncidentMapper preuveIncidentMapper;

    @PatchMapping("/disponibilite")
    public ResponseEntity<ProfilLivreurResponse> basculerDisponibilite(
            @Valid @RequestBody DisponibiliteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ProfilLivreur profil = livreurService.basculerDisponibilite(userDetails.getUtilisateur(), request.enLigne());
        return ResponseEntity.ok(livreurMapper.toResponse(profil));
    }

    @PostMapping("/position")
    public ResponseEntity<Void> actualiserPositionGps(
            @Valid @RequestBody PositionGpsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        livreurService.actualiserPositionGpsPendantCourse(
                userDetails.getUtilisateur(), request.latitude(), request.longitude()
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/offres")
    public ResponseEntity<List<OffreCourseLivreurResponse>> listerOffresEnAttente(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ProfilLivreur profil = profilLivreurRepository.findByUtilisateurId(userDetails.getUtilisateur().getId())
                .orElseThrow(() -> new BusinessRuleException("Profil livreur introuvable."));

        List<OffreCourse> offres = offreCourseRepository.findByLivreurIdAndStatutReponse(
                profil.getId(), StatutOffre.ENVOYEE
        );
        return ResponseEntity.ok(dispatchMapper.toLivreurResponseList(offres));
    }

    @PostMapping("/offres/{offreId}/repondre")
    public ResponseEntity<OffreCourseLivreurResponse> repondreOffre(
            @PathVariable UUID offreId,
            @Valid @RequestBody ReponseOffreRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        OffreCourse offre = matchingDispatchService.repondreAOffre(
                offreId, userDetails.getUtilisateur(), request.acceptee(), request.motifRefus()
        );
        return ResponseEntity.ok(dispatchMapper.toLivreurResponse(offre));
    }

    @PostMapping("/commandes/{commandeId}/collecte")
    public ResponseEntity<CommandeExpediteurResponse> confirmerCollecte(
            @PathVariable UUID commandeId,
            @Valid @RequestBody PreuveSubmitRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Commande cmd = commandeService.confirmerCollecte(
                commandeId,
                userDetails.getUtilisateur(),
                request.typePreuve(),
                request.valeurOuFichierUrl(),
                request.latitudeCapture(),
                request.longitudeCapture()
        );
        return ResponseEntity.ok(commandeMapper.toExpediteurResponse(cmd));
    }

    @PostMapping("/commandes/{commandeId}/demarrer-livraison")
    public ResponseEntity<Map<String, Object>> demarrerLivraison(
            @PathVariable UUID commandeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String codePinGenere = commandeService.demarrerLivraisonEtGenererPin(commandeId, userDetails.getUtilisateur());
        return ResponseEntity.ok(Map.of(
                "statut", "EN_LIVRAISON",
                "message", "Livraison démarrée. Code PIN généré pour notification destinataire.",
                "codePinDebug", codePinGenere // À retirer en production une fois le SMS branché
        ));
    }

    @PostMapping("/commandes/{commandeId}/livrer")
    public ResponseEntity<CommandeExpediteurResponse> confirmerLivraisonEtCloturer(
            @PathVariable UUID commandeId,
            @Valid @RequestBody PreuveSubmitRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Commande cmd = commandeService.confirmerLivraisonEtCloturer(
                commandeId,
                userDetails.getUtilisateur(),
                request.typePreuve(),
                request.valeurOuFichierUrl(),
                request.latitudeCapture(),
                request.longitudeCapture()
        );
        return ResponseEntity.ok(commandeMapper.toExpediteurResponse(cmd));
    }

    @PostMapping("/commandes/{commandeId}/incidents")
    public ResponseEntity<IncidentResponse> declarerIncident(
            @PathVariable UUID commandeId,
            @Valid @RequestBody IncidentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        IncidentLitige incident = commandeService.declarerEchecOuIncident(
                commandeId,
                userDetails.getUtilisateur(),
                request.motif(),
                request.description(),
                request.pieceJointePhotoUrl()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(preuveIncidentMapper.toIncidentResponse(incident));
    }
}