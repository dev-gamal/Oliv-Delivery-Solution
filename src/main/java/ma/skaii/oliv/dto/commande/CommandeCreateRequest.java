package ma.skaii.oliv.dto.commande;

import jakarta.validation.constraints.*;
import ma.skaii.oliv.domain.enums.ModePaiement;
import ma.skaii.oliv.domain.enums.TypeDemande;
import ma.skaii.oliv.domain.enums.TypeVehicule;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CommandeCreateRequest(
        @NotNull TypeDemande typeDemande,
        @NotNull TypeVehicule vehiculeRequis,
        @NotBlank String zone,

        // Point de collecte
        @NotBlank String adresseCollecte,
        @NotNull Double latitudeCollecte,
        @NotNull Double longitudeCollecte,
        String contactCollecteNom,
        String contactCollecteTel,
        String instructionsCollecte,
        String referenceInterneCommercant, // Cas restaurant / boutique (Section 5 & 6.2)

        // Point de livraison & Destinataire
        @NotBlank String adresseLivraison,
        @NotNull Double latitudeLivraison,
        @NotNull Double longitudeLivraison,
        @NotBlank String destinataireNom,
        @NotBlank String destinataireTelephone,
        String instructionsLivraison,

        // Contenu & Conformité (Section 12.1)
        @NotBlank String natureContenu,
        @AssertTrue(message = "Vous devez certifier que le colis ne contient aucune marchandise interdite")
        boolean declarationMarchandiseAutorisee,
        @PositiveOrZero Double poidsKg,
        String dimensionsCm,
        @PositiveOrZero BigDecimal valeurDeclareeDh,

        // Distance & Options (Section 9.4)
        @NotNull @Positive BigDecimal distanceItineraireKm,
        boolean optionExpress,
        boolean optionCreneauGaranti,
        OffsetDateTime creneauDebut,
        OffsetDateTime creneauFin,
        @Min(0) int nombreArretsSupplementaires,
        boolean retourPointCollecteRequis,

        // Paiement & COD
        @NotNull ModePaiement modePaiement,
        @PositiveOrZero BigDecimal montantCodMarchandiseDh
) {}
