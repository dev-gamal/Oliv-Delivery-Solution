package ma.skaii.oliv.dto.preuve;

import ma.skaii.oliv.domain.enums.EtapePreuve;
import ma.skaii.oliv.domain.enums.TypePreuve;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PreuveResponse(
        UUID id,
        UUID commandeId,
        EtapePreuve etape,
        TypePreuve typePreuve,
        String valeurOuFichierUrl,
        String auteurNom,
        OffsetDateTime horodatage
) {}