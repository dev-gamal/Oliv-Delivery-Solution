package ma.skaii.oliv.dto.commande;

import ma.skaii.oliv.domain.enums.StatutCommande;
import ma.skaii.oliv.domain.enums.TypeDemande;
import ma.skaii.oliv.domain.enums.TypeVehicule;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CommandeExpediteurResponse(
        UUID id,
        String referenceUnique,
        TypeDemande typeDemande,
        TypeVehicule vehiculeRequis,
        StatutCommande statut,
        String zone,
        String adresseCollecte,
        String adresseLivraison,
        String destinataireNom,
        String destinataireTelephone,
        String referenceInterneCommercant,
        boolean commandeSignaleePrete,
        BigDecimal distanceFactureeKm,
        String tokenSuiviDestinataire,
        String livreurPrenom,
        DetailFinancierClientResponse detailFinancier,
        List<HistoriqueStatutResponse> historiqueStatuts,
        OffsetDateTime dateCreation,
        OffsetDateTime dateLivraisonEffective
) {}