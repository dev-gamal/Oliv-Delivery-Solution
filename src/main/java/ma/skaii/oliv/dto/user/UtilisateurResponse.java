package ma.skaii.oliv.dto.user;

import ma.skaii.oliv.domain.enums.RoleUtilisateur;
import ma.skaii.oliv.domain.enums.StatutCompte;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UtilisateurResponse(
        UUID id,
        String nomComplet,
        String telephone,
        String email,
        RoleUtilisateur role,
        StatutCompte statut,
        boolean telephoneVerifie,
        UUID organisationId,
        OffsetDateTime dateCreation
) {}
