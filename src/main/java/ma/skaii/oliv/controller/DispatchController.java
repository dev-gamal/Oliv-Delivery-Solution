package ma.skaii.oliv.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.OffreCourse;
import ma.skaii.oliv.domain.entity.ProfilLivreur;
import ma.skaii.oliv.domain.enums.StatutCommande;
import ma.skaii.oliv.dto.commande.CommandeAdminFinanceResponse;
import ma.skaii.oliv.dto.dispatch.EnvoyerOffreRequest;
import ma.skaii.oliv.dto.dispatch.LivreurCandidatResponse;
import ma.skaii.oliv.dto.dispatch.OffreCourseLivreurResponse;
import ma.skaii.oliv.dto.livreur.ProfilLivreurResponse;
import ma.skaii.oliv.dto.livreur.ValidationKycRequest;
import ma.skaii.oliv.mapper.CommandeMapper;
import ma.skaii.oliv.mapper.DispatchMapper;
import ma.skaii.oliv.mapper.LivreurMapper;
import ma.skaii.oliv.repository.CommandeRepository;
import ma.skaii.oliv.security.CustomUserDetails;
import ma.skaii.oliv.service.LivreurService;
import ma.skaii.oliv.service.MatchingDispatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final MatchingDispatchService matchingDispatchService;
    private final LivreurService livreurService;
    private final CommandeRepository commandeRepository;
    private final CommandeMapper commandeMapper;
    private final LivreurMapper livreurMapper;
    private final DispatchMapper dispatchMapper;

    @GetMapping("/commandes")
    public ResponseEntity<List<CommandeAdminFinanceResponse>> listerCommandesParZoneEtStatut(
            @RequestParam String zone,
            @RequestParam(defaultValue = "EN_RECHERCHE_LIVREUR") StatutCommande statut
    ) {
        List<CommandeAdminFinanceResponse> list = commandeRepository
                .findByZoneAndStatutOrderByDateCreationAsc(zone, statut)
                .stream()
                .map(commandeMapper::toAdminFinanceResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/commandes/{commandeId}/candidats")
    public ResponseEntity<List<LivreurCandidatResponse>> listerCandidats(@PathVariable UUID commandeId) {
        List<LivreurCandidatResponse> candidats = matchingDispatchService
                .rechercherLivreursCandidatsClasses(commandeId)
                .stream()
                .map(l -> livreurMapper.toCandidatResponse(l, null))
                .toList();
        return ResponseEntity.ok(candidats);
    }

    @PostMapping("/offres")
    public ResponseEntity<OffreCourseLivreurResponse> envoyerOffre(
            @Valid @RequestBody EnvoyerOffreRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        OffreCourse offre = matchingDispatchService.envoyerOffreManuelle(
                request.commandeId(), request.profilLivreurId(), userDetails.getUtilisateur()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dispatchMapper.toLivreurResponse(offre));
    }

    @PatchMapping("/livreurs/{profilLivreurId}/valider-kyc")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfilLivreurResponse> validerKycLivreur(
            @PathVariable UUID profilLivreurId,
            @Valid @RequestBody ValidationKycRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        ProfilLivreur profil = livreurService.validerOnboardingLivreur(
                profilLivreurId, userDetails.getUtilisateur(), request.motif(), httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(livreurMapper.toResponse(profil));
    }
}