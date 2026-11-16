package ma.skaii.oliv.dto.livreur;

import jakarta.validation.constraints.NotNull;

public record DisponibiliteRequest(
        @NotNull Boolean enLigne
) {}