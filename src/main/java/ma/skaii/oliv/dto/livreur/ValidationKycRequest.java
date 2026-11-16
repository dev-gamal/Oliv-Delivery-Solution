package ma.skaii.oliv.dto.livreur;

import jakarta.validation.constraints.NotBlank;

public record ValidationKycRequest(
        @NotBlank(message = "Un motif de validation est obligatoire pour l'audit")
        String motif
) {}