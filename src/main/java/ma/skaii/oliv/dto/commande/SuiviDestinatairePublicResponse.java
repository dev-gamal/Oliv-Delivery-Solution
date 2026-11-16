package ma.skaii.oliv.dto.commande;

import ma.skaii.oliv.domain.enums.StatutCommande;
import ma.skaii.oliv.domain.enums.TypeVehicule;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Vue publique sans compte pour le destinataire via /api/public/suivi/{token} (Section 3 & Règle n°8).
 */
public record SuiviDestinatairePublicResponse(
        String referenceUnique,
        StatutCommande statut,
        String expediteurNomOuEnseigne,
        String adresseLivraison,
        String livreurPrenom,
        TypeVehicule vehicule,
        Double livreurLatitudeActuelle, // Uniquement si EN_LIVRAISON (Section 7.4)
        Double livreurLongitudeActuelle,
        OffsetDateTime dateDernierePosition,
        List<HistoriqueStatutResponse> etapes
) {}