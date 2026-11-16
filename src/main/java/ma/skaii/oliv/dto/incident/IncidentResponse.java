package ma.skaii.oliv.dto.incident;

import ma.skaii.oliv.domain.enums.MotifIncident;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        UUID commandeId,
        String referenceCommande,
        String declarantNom,
        MotifIncident motif,
        String description,
        String pieceJointePhotoUrl,
        String decisionOperateur,
        BigDecimal montantRemboursementClientDh,
        BigDecimal indemniteTempsEngageLivreurDh,
        OffsetDateTime dateDeclaration,
        OffsetDateTime dateResolution
) {}