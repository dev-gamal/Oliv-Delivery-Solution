package ma.skaii.oliv.dto.user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OrganisationProResponse(
        UUID id,
        String raisonSociale,
        String ice,
        String emailFacturation,
        String planAbonnementActif,
        BigDecimal prixMensuelAbonnement,
        Integer quotaCoursesInclusesMensuel,
        Integer coursesConsommeesMoisCourant,
        BigDecimal fraisServiceSousContrat,
        LocalDate dateRenouvellementAbonnement
) {}