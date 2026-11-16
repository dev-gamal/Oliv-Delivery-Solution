package ma.skaii.oliv.dto.qualite;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NoteAvisResponse(
        UUID id,
        UUID commandeId,
        String auteurNom,
        String cibleNom,
        int noteSurCinq,
        String commentaire,
        OffsetDateTime dateCreation
) {}