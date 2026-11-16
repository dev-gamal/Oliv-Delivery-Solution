package ma.skaii.oliv.dto.dispatch;

import ma.skaii.oliv.domain.enums.TypeVehicule;

import java.math.BigDecimal;
import java.util.UUID;

public record LivreurCandidatResponse(
        UUID profilLivreurId,
        String nomComplet,
        TypeVehicule vehiculeDeclare,
        String zoneAffectation,
        Double distanceApprocheKm,
        BigDecimal noteMoyenne,
        BigDecimal tauxAcceptation,
        boolean enLigne
) {}