package ma.skaii.oliv.dto.auth;

import ma.skaii.oliv.domain.enums.RoleUtilisateur;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UUID utilisateurId,
        String nomComplet,
        RoleUtilisateur role
) {}
