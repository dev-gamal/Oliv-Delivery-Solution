package ma.skaii.oliv.dto.incident;

import jakarta.validation.constraints.NotNull;
import ma.skaii.oliv.domain.enums.MotifIncident;

public record IncidentCreateRequest(
        @NotNull(message = "Un motif normalisé est obligatoire (Section 6.1 & 15)")
        MotifIncident motif,
        String description,
        String pieceJointePhotoUrl
) {}