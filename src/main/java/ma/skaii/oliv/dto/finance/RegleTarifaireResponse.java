package ma.skaii.oliv.dto.finance;

import ma.skaii.oliv.domain.enums.TypeVehicule;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RegleTarifaireResponse(
        UUID id,
        String codeVersion,
        String zone,
        TypeVehicule vehicule,
        BigDecimal prixTranche0a2Km,
        BigDecimal prixTranche2a4Km,
        BigDecimal prixTranche4a6Km,
        BigDecimal prixTranche6a8Km,
        BigDecimal coeffKmSupplementaire,
        BigDecimal fraisServiceStandard,
        BigDecimal pourcentageGainLivreur,
        BigDecimal pourcentageCommissionOliv,
        OffsetDateTime valideDepuis,
        boolean actif
) {}