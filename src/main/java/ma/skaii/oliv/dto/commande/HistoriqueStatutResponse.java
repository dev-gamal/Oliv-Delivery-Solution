package ma.skaii.oliv.dto.commande;

import ma.skaii.oliv.domain.enums.StatutCommande;

import java.time.OffsetDateTime;

public record HistoriqueStatutResponse(
        StatutCommande ancienStatut,
        StatutCommande nouveauStatut,
        OffsetDateTime horodatage,
        String auteurNom,
        String motifOuCommentaire
) {}
