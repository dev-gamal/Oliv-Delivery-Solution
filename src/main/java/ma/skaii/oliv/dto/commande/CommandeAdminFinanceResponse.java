package ma.skaii.oliv.dto.commande;

import ma.skaii.oliv.domain.enums.StatutCommande;
import ma.skaii.oliv.domain.enums.TypeDemande;
import ma.skaii.oliv.domain.enums.TypeVehicule;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CommandeAdminFinanceResponse(
        UUID id,
        String referenceUnique,
        TypeDemande typeDemande,
        TypeVehicule vehiculeRequis,
        StatutCommande statut,
        String zone,
        UUID expediteurId,
        String expediteurNom,
        UUID organisationId,
        UUID livreurAffecteId,
        String livreurNom,
        BigDecimal distanceItineraireKm,
        BigDecimal distanceFactureeKm,
        DetailFinancierAdminResponse detailFinancierAdmin,
        OffsetDateTime dateCreation,
        OffsetDateTime dateCloture
) {}