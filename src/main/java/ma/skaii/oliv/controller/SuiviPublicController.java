package ma.skaii.oliv.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.dto.commande.SuiviDestinatairePublicResponse;
import ma.skaii.oliv.dto.tracking.TrackingPositionEvent;
import ma.skaii.oliv.mapper.CommandeMapper;
import ma.skaii.oliv.repository.CommandeRepository;
import ma.skaii.oliv.service.TrackingTempsReelService;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/suivi")
@RequiredArgsConstructor
@SecurityRequirements
public class SuiviPublicController {

    private final CommandeRepository commandeRepository;
    private final CommandeMapper commandeMapper;
    private final TrackingTempsReelService trackingTempsReelService; // <-- Injection ici

    @GetMapping("/{token}")
    public ResponseEntity<SuiviDestinatairePublicResponse> suivreCommande(@PathVariable String token) {
        Commande commande = commandeRepository.findByTokenSuiviDestinataire(token)
                .orElseThrow(() -> new BusinessRuleException("Lien de suivi invalide ou introuvable."));
        return ResponseEntity.ok(commandeMapper.toSuiviPublicResponse(commande));
    }

    @GetMapping("/{token}/position")
    public ResponseEntity<TrackingPositionEvent> obtenirDernierePosition(@PathVariable String token) {
        return ResponseEntity.ok(trackingTempsReelService.obtenirDernierePositionParToken(token));
    }
}