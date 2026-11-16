package ma.skaii.oliv.dto.dispatch;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EnvoyerOffreRequest(
        @NotNull UUID commandeId,
        @NotNull UUID profilLivreurId
) {}
