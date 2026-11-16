package ma.skaii.oliv.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VersementLivreurResponse(
        UUID id,
        UUID livreurId,
        String livreurNom,
        String referencePaiement,
        LocalDate periodeDebut,
        LocalDate periodeFin,
        BigDecimal montantGainsCoursesDh,
        BigDecimal montantPrimesDh,
        BigDecimal montantTotalVerseDh,
        String statutVersement,
        String modeVersement,
        OffsetDateTime dateExecution
) {}