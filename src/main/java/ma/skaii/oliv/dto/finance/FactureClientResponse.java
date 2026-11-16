package ma.skaii.oliv.dto.finance;

import ma.skaii.oliv.domain.enums.StatutPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FactureClientResponse(
        UUID id,
        String numeroFacture,
        String clientNomOuRaisonSociale,
        LocalDate dateEmission,
        LocalDate dateEcheance,
        BigDecimal montantHtDh,
        BigDecimal montantTvaDh,
        BigDecimal montantTtcDh,
        StatutPaiement statut,
        String fichierPdfUrl
) {}