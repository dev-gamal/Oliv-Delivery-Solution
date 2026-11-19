package ma.skaii.oliv.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.entity.DetailFinancierCommande;
import ma.skaii.oliv.domain.entity.RegleTarifaire;
import ma.skaii.oliv.domain.entity.VersementLivreur;
import ma.skaii.oliv.dto.commande.DetailFinancierAdminResponse;
import ma.skaii.oliv.dto.finance.*;
import ma.skaii.oliv.mapper.CommandeMapper;
import ma.skaii.oliv.mapper.FinanceMapper;
import ma.skaii.oliv.repository.CommandeRepository;
import ma.skaii.oliv.repository.RegleTarifaireRepository;
import ma.skaii.oliv.security.CustomUserDetails;
import ma.skaii.oliv.service.FinanceReportingService;
import ma.skaii.oliv.service.PricingEngineService;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceReportingService financeReportingService;
    private final PricingEngineService pricingEngineService;
    private final RegleTarifaireRepository regleTarifaireRepository;
    private final CommandeRepository commandeRepository;
    private final FinanceMapper financeMapper;
    private final CommandeMapper commandeMapper;

    @GetMapping("/kpi")
    public ResponseEntity<SyntheseFinanciereKpiResponse> obtenirSyntheseKpi(
            @RequestParam(defaultValue = "30000") BigDecimal chargesFixesMensuellesDh
    ) {
        var kpi = financeReportingService.calculerSyntheseFinanciere(chargesFixesMensuellesDh);
        return ResponseEntity.ok(financeMapper.toKpiResponse(kpi));
    }

    @PostMapping("/versements-livreurs")
    public ResponseEntity<VersementLivreurResponse> executerVersement(
            @Valid @RequestBody VersementLivreurRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        VersementLivreur versement = financeReportingService.executerVersementLivreur(
                request.profilLivreurId(),
                request.periodeDebut(),
                request.periodeFin(),
                request.primesEventuellesDh(),
                request.modeVersement(),
                userDetails.getUtilisateur(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(financeMapper.toVersementResponse(versement));
    }

    @PostMapping("/regles-tarifaires")
    public ResponseEntity<RegleTarifaireResponse> creerNouvelleVersionTarifaire(
            @Valid @RequestBody RegleTarifaireRequest request
    ) {
        // Désactiver l'ancienne règle active pour cette zone + véhicule (versioning immuable)
        regleTarifaireRepository.findByZoneAndVehiculeAndActifTrue(request.zone(), request.vehicule())
                .ifPresent(ancienne -> {
                    ancienne.setActif(false);
                    ancienne.setValideJusqua(OffsetDateTime.now());
                    regleTarifaireRepository.save(ancienne);
                });

        RegleTarifaire nouvelle = financeMapper.toEntity(request);
        nouvelle.setValideDepuis(OffsetDateTime.now());
        nouvelle.setActif(true);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financeMapper.toRegleResponse(regleTarifaireRepository.save(nouvelle)));
    }

    @PatchMapping("/commandes/{commandeId}/ajuster-prix")
    public ResponseEntity<DetailFinancierAdminResponse> ajusterPrixCommande(
            @PathVariable UUID commandeId,
            @Valid @RequestBody AjustementPrixRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new BusinessRuleException("Commande introuvable."));

        DetailFinancierCommande updated = pricingEngineService.ajusterPrixApresValidation(
                commande,
                request.nouveauTotalClientDh(),
                request.fraisEchecSuppDh(),
                userDetails.getUtilisateur(),
                request.motifObligatoire(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(commandeMapper.toDetailAdminResponse(updated));
    }
}