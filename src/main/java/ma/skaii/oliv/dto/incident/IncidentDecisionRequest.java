package ma.skaii.oliv.dto.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record IncidentDecisionRequest(
        @NotBlank String decisionOperateur, // NOUVELLE_TENTATIVE, RETOUR_EXPEDITEUR, ANNULATION, CLOTURE
        @PositiveOrZero BigDecimal montantRemboursementClientDh,
        @PositiveOrZero BigDecimal indemniteTempsEngageLivreurDh
) {}