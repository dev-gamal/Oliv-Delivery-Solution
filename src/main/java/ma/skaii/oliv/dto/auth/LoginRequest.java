package ma.skaii.oliv.dto.auth;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank(message = "Téléphone ou e-mail obligatoire")
        String identifiant,

        @NotBlank(message = "Mot de passe obligatoire")
        String motDePasse
) {}