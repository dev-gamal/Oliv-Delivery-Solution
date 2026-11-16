package ma.skaii.oliv.dto.qualite;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NoteAvisRequest(
        @NotNull UUID commandeId,
        @NotNull UUID cibleId,
        @Min(1) @Max(5) int noteSurCinq,
        String commentaire
) {}