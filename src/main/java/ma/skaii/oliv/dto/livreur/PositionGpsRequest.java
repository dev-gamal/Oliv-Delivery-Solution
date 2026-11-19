package ma.skaii.oliv.dto.livreur;

import jakarta.validation.constraints.*;

public record PositionGpsRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @DecimalMin("0.0") @DecimalMax("360.0") Double capDegres,
        @PositiveOrZero Double vitesseKmh
) {}