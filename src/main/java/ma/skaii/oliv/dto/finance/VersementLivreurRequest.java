package ma.skaii.oliv.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VersementLivreurRequest(
        @NotNull UUID profilLivreurId,
        @NotNull LocalDate periodeDebut,
        @NotNull LocalDate periodeFin,
        @PositiveOrZero BigDecimal primesEventuellesDh,
        @NotBlank String modeVersement // VIREMENT_BANCAIRE, ESPECES_AGENCE
) {}