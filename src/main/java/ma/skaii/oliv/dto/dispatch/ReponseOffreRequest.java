package ma.skaii.oliv.dto.dispatch;

import jakarta.validation.constraints.NotNull;

public record ReponseOffreRequest(
        @NotNull Boolean acceptee,
        String motifRefus // Optionnel si acceptee == true
) {}