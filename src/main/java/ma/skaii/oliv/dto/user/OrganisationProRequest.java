package ma.skaii.oliv.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OrganisationProRequest(
        @NotBlank String raisonSociale,
        String ice,
        @NotBlank String nomContactPrincipal,
        @NotBlank String telephoneContact,
        @Email String emailFacturation,
        @NotBlank String adresseFacturation,
        String planAbonnementActif // START_20, BUSINESS_50, PRO_100, MIX_100
) {}