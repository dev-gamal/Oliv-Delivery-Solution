package ma.skaii.oliv.dto.auth;

import jakarta.validation.constraints.*;
import ma.skaii.oliv.domain.enums.RoleUtilisateur;
import java.util.UUID;

public record RegisterRequest(
        @NotBlank(message = "Le nom complet est obligatoire")
        String nomComplet,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        @Pattern(regexp = "^(\\+212|0)[5-7][0-9]{8}$", message = "Format de téléphone marocain invalide")
        String telephone,

        @Email(message = "Format d'e-mail invalide")
        String email,

        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String motDePasse,

        @NotNull
        RoleUtilisateur role,

        @AssertTrue(message = "L'acceptation des conditions d'utilisation est obligatoire")
        boolean cguAcceptees
) {}
