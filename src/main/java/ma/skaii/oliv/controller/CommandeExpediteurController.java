package ma.skaii.oliv.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.entity.IncidentLitige;
import ma.skaii.oliv.dto.commande.CommandeCreateRequest;
import ma.skaii.oliv.dto.commande.CommandeExpediteurResponse;
import ma.skaii.oliv.dto.incident.IncidentCreateRequest;
import ma.skaii.oliv.dto.incident.IncidentResponse;
import ma.skaii.oliv.mapper.CommandeMapper;
import ma.skaii.oliv.mapper.PreuveIncidentMapper;
import ma.skaii.oliv.security.CustomUserDetails;
import ma.skaii.oliv.service.CommandeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expediteur/commandes")
@RequiredArgsConstructor
public class CommandeExpediteurController {

    private final CommandeService commandeService;
    private final CommandeMapper commandeMapper;
    private final PreuveIncidentMapper preuveIncidentMapper;

    @PostMapping
    public ResponseEntity<CommandeExpediteurResponse> creerEtEstimer(
            @Valid @RequestBody CommandeCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Commande brouillon = commandeMapper.toEntity(request);
        Commande saved = commandeService.creerBrouillonEtEstimer(
                brouillon,
                userDetails.getUtilisateur(),
                request.modePaiement(),
                request.montantCodMarchandiseDh()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(commandeMapper.toExpediteurResponse(saved));
    }

    @PostMapping("/{id}/confirmer")
    public ResponseEntity<CommandeExpediteurResponse> confirmer(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Commande confirmee = commandeService.confirmerCommande(id, userDetails.getUtilisateur());
        return ResponseEntity.ok(commandeMapper.toExpediteurResponse(confirmee));
    }

    @PatchMapping("/{id}/prete")
    public ResponseEntity<CommandeExpediteurResponse> signalerPrete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Commande cmd = commandeService.signalerCommandePreteCommercant(id, userDetails.getUtilisateur());
        return ResponseEntity.ok(commandeMapper.toExpediteurResponse(cmd));
    }

    @GetMapping
    public ResponseEntity<List<CommandeExpediteurResponse>> listerMesCommandes(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<CommandeExpediteurResponse> responses = commandeService
                .listerMesCommandesExpediteur(userDetails.getUtilisateur())
                .stream()
                .map(commandeMapper::toExpediteurResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeExpediteurResponse> consulterCommande(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Commande cmd = commandeService.getCommandePourExpediteur(id, userDetails.getUtilisateur());
        return ResponseEntity.ok(commandeMapper.toExpediteurResponse(cmd));
    }

    @PostMapping("/{id}/incidents")
    public ResponseEntity<IncidentResponse> signalerIncident(
            @PathVariable UUID id,
            @Valid @RequestBody IncidentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        IncidentLitige incident = commandeService.declarerEchecOuIncident(
                id,
                userDetails.getUtilisateur(),
                request.motif(),
                request.description(),
                request.pieceJointePhotoUrl()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(preuveIncidentMapper.toIncidentResponse(incident));
    }
}