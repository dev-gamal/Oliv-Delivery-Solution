package ma.skaii.oliv.dto.commande;

import java.math.BigDecimal;

/**
 * Vue complète réservée aux rôles FINANCE et ADMIN (Section 10.2).
 */
public record DetailFinancierAdminResponse(
        DetailFinancierClientResponse detailClient,
        BigDecimal gainNetLivreurDh,
        BigDecimal commissionOlivSurLivraisonDh,
        BigDecimal revenuPropreOlivDh,
        BigDecimal coutsVariablesReserveDh,
        BigDecimal margeContributiveDh,
        boolean montantCodReverseExpediteur,
        String referenceRapprochementPaiement
) {}