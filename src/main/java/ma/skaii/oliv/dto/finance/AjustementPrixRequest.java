package ma.skaii.oliv.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Règle d'or n°7 : Ajustement de prix après validation avec motif obligatoire.
 */
public record AjustementPrixRequest(
        @NotNull @PositiveOrZero BigDecimal nouveauTotalClientDh,
        @NotNull @PositiveOrZero BigDecimal fraisEchecSuppDh,
        @NotBlank(message = "Le motif est obligatoire pour toute modification de prix après validation")
        String motifObligatoire
) {}