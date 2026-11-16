package ma.skaii.oliv.dto.livreur;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ma.skaii.oliv.domain.enums.TypeVehicule;

public record OnboardingLivreurRequest(
        @NotBlank String zoneAffectation,
        @NotNull TypeVehicule vehiculeDeclare,
        @NotBlank String pieceIdentiteUrl,
        String permisConduireUrl, // Obligatoire si MOTO, AUTO ou PRO (Section 3)
        String justificatifStatutAutoEntrepreneurUrl
) {}