package ma.skaii.oliv.dto.finance;

import jakarta.validation.constraints.*;
import ma.skaii.oliv.domain.enums.TypeVehicule;

import java.math.BigDecimal;

/**
 * Paramétrage tarifaire sans code (Règle n°10 & Section 9.8).
 */
public record RegleTarifaireRequest(
        @NotBlank String codeVersion,
        @NotBlank String zone,
        @NotNull TypeVehicule vehicule,
        @NotNull @Positive BigDecimal prixTranche0a2Km,
        @NotNull @Positive BigDecimal prixTranche2a4Km,
        @NotNull @Positive BigDecimal prixTranche4a6Km,
        @NotNull @Positive BigDecimal prixTranche6a8Km,
        @NotNull @Positive BigDecimal distanceIncluseKm,
        @NotNull @Positive BigDecimal coeffKmSupplementaire,
        boolean utiliserGrilleJusqua8Km,
        @NotNull @PositiveOrZero BigDecimal fraisServiceStandard,
        @NotNull @PositiveOrZero BigDecimal fraisServiceExpress,
        @NotNull @PositiveOrZero BigDecimal supplementOptionExpress,
        @NotNull @PositiveOrZero BigDecimal supplementCreneauGaranti,
        @NotNull @PositiveOrZero BigDecimal tarifAttentePar10Min,
        @NotNull @PositiveOrZero BigDecimal supplementArretSimple,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal pourcentageGainLivreur,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal pourcentageCommissionOliv,
        @NotNull @PositiveOrZero BigDecimal reserveSupportPaiementParCourse
) {}