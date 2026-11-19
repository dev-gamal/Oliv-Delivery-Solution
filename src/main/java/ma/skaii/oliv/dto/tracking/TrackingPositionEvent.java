package ma.skaii.oliv.dto.tracking;

import ma.skaii.oliv.domain.enums.StatutCommande;
import java.time.OffsetDateTime;

/**
 * Payload léger diffusé toutes les 3 à 5 secondes vers la carte client.
 */
public record TrackingPositionEvent(
        String referenceCommande,
        StatutCommande statut,
        Double latitude,
        Double longitude,
        Double capDegres,       // Optionnel : permet d'orienter l'icône moto/vélo sur la carte
        Double vitesseKmh,      // Optionnel : estimation ETA
        OffsetDateTime horodatage
) {}