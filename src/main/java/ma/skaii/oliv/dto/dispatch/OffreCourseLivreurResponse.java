package ma.skaii.oliv.dto.dispatch;

import ma.skaii.oliv.domain.enums.ModeDispatch;
import ma.skaii.oliv.domain.enums.StatutOffre;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OffreCourseLivreurResponse(
        UUID offreId,
        UUID commandeId,
        String referenceCommande,
        String adresseCollecte,
        String adresseLivraison,
        BigDecimal distanceApprocheCollecteKm,
        BigDecimal distanceLivraisonKm,
        BigDecimal gainNetEstimeAfficheDh, // Transparence avant acceptation (Section 2.2 & 7.2)
        ModeDispatch modeDispatch,
        StatutOffre statutReponse,
        OffsetDateTime heureEnvoi,
        OffsetDateTime heureExpiration,
        int delaiReponseSecondes
) {}