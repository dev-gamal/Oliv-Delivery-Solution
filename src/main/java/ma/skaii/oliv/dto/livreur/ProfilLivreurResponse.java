package ma.skaii.oliv.dto.livreur;

import ma.skaii.oliv.domain.enums.StatutVerificationLivreur;
import ma.skaii.oliv.domain.enums.TypeVehicule;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfilLivreurResponse(
        UUID id,
        UUID utilisateurId,
        String nomComplet,
        String telephone,
        String zoneAffectation,
        TypeVehicule vehiculeDeclare,
        boolean enLigne,
        StatutVerificationLivreur statutVerification,
        boolean formationCourteValidee,
        BigDecimal noteMoyenne,
        BigDecimal tauxAcceptation,
        BigDecimal soldeAPercevoirDh,
        OffsetDateTime dateDernierePosition
) {}