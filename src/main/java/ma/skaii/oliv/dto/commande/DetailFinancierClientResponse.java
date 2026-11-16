package ma.skaii.oliv.dto.commande;

import ma.skaii.oliv.domain.enums.ModePaiement;
import ma.skaii.oliv.domain.enums.StatutPaiement;

import java.math.BigDecimal;

/**
 * Récapitulatif transparent affiché à l'expéditeur avant confirmation (Règles n°2, n°3 et n°4).
 */
public record DetailFinancierClientResponse(
        String versionRegleTarifaire,
        BigDecimal prixLivraisonBaseDh,
        BigDecimal fraisServiceDh, // Ligne distincte obligatoire
        BigDecimal supplementExpressDh,
        BigDecimal supplementAttenteDh,
        BigDecimal supplementArretsDh,
        BigDecimal fraisEchecOuRetourDh,
        BigDecimal remiseAppliqueeDh,
        String codePromoOuMotifRemise,
        BigDecimal totalPayeExpediteurDh,
        BigDecimal montantCodAEncaisserMarchandiseDh,
        ModePaiement modePaiement,
        StatutPaiement statutPaiement
) {}