package ma.skaii.oliv.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, String> validationErrors
) {}