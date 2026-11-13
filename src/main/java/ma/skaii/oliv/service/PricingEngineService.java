package ma.skaii.oliv.service;

import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.*;
import ma.skaii.oliv.domain.enums.ModePaiement;
import ma.skaii.oliv.domain.enums.StatutPaiement;
import ma.skaii.oliv.domain.enums.TypeVehicule;
import ma.skaii.oliv.repository.DetailFinancierCommandeRepository;
import ma.skaii.oliv.repository.RegleTarifaireRepository;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PricingEngineService {

    private final RegleTarifaireRepository regleTarifaireRepository;
    private final DetailFinancierCommandeRepository detailFinancierRepository;
    private final AuditService auditService;

    private static final BigDecimal DEUX = new BigDecimal("2.00");
    private static final BigDecimal QUATRE = new BigDecimal("4.00");
    private static final BigDecimal SIX = new BigDecimal("6.00");
    private static final BigDecimal HUIT = new BigDecimal("8.00");
    private static final BigDecimal CENT = new BigDecimal("100.00");

    /**
     * Arrondit une valeur (distance en km ou montant en DH) au 0,5 supérieur (Section 19.13).
     */
    public BigDecimal arrondirAuDemiSuperieur(BigDecimal valeur) {
        if (valeur == null || valeur.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return valeur.multiply(DEUX)
                .setScale(0, RoundingMode.CEILING)
                .divide(DEUX, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule et fige le détail financier complet d'une commande (Sections 9.8 et 19).
     */
    @Transactional
    public DetailFinancierCommande calculerEtFigerTarif(
            Commande commande,
            ModePaiement modePaiement,
            BigDecimal montantCodMarchandiseDh
    ) {
        if (commande.getVehiculeRequis() == TypeVehicule.PRO) {
            throw new BusinessRuleException("Le véhicule PRO Utilitaire nécessite un traitement sur devis.");
        }

        RegleTarifaire regle = regleTarifaireRepository
                .findByZoneAndVehiculeAndActifTrue(commande.getZone(), commande.getVehiculeRequis())
                .orElseThrow(() -> new BusinessRuleException(
                        "Aucune règle tarifaire active pour la zone " + commande.getZone()
                                + " et le véhicule " + commande.getVehiculeRequis()));

        // 1. Distance facturée arrondie au 0,5 km supérieur (minimum 2 km inclus)
        BigDecimal distanceFacturee = arrondirAuDemiSuperieur(commande.getDistanceItineraireKm());
        commande.setDistanceFactureeKm(distanceFacturee);

        // 2. Calcul du prix de livraison de base (P)
        BigDecimal prixLivraisonBase = calculerPrixBaseSelonDistance(distanceFacturee, regle);

        // 3. Frais de service en ligne distincte (Règle n°3)
        BigDecimal fraisService = commande.isOptionExpress()
                ? regle.getFraisServiceExpress()
                : regle.getFraisServiceStandard();

        // Vérification abonnement B2B (Section 9.3, 9.6 et 19.10)
        OrganisationExpediteur org = commande.getOrganisationPro();
        boolean courseIncluseAbonnement = false;
        if (org != null && org.getPlanAbonnementActif() != null
                && org.getQuotaCoursesInclusesMensuel() != null
                && org.getCoursesConsommeesMoisCourant() < org.getQuotaCoursesInclusesMensuel()
                && distanceFacturee.compareTo(DEUX) <= 0
                && commande.getVehiculeRequis() == TypeVehicule.ECO) {
            courseIncluseAbonnement = true;
            fraisService = org.getFraisServiceSousContrat() != null
                    ? org.getFraisServiceSousContrat()
                    : BigDecimal.ZERO;
        }

        // 4. Options payantes (Section 9.4)
        BigDecimal suppExpress = commande.isOptionExpress() ? regle.getSupplementOptionExpress() : BigDecimal.ZERO;
        BigDecimal suppCreneau = commande.isOptionCreneauGaranti() ? regle.getSupplementCreneauGaranti() : BigDecimal.ZERO;
        BigDecimal suppArrets = regle.getSupplementArretSimple()
                .multiply(BigDecimal.valueOf(commande.getNombreArretsSupplementaires()));

        // Attente > 10 min (franchise de 10 min, puis par tranche de 10 min)
        BigDecimal suppAttente = BigDecimal.ZERO;
        if (commande.getMinutesAttenteCollecte() > 10) {
            int tranchesAttente = (int) Math.ceil((commande.getMinutesAttenteCollecte() - 10) / 10.0);
            suppAttente = regle.getTarifAttentePar10Min().multiply(BigDecimal.valueOf(tranchesAttente));
        }

        // Course retour / aller-retour (Section 5 & 9.4)
        BigDecimal fraisRetour = commande.isRetourPointCollecteRequis() ? prixLivraisonBase : BigDecimal.ZERO;

        // 5. Remises hors abonnement par volume mensuel B2B (Section 9.6 & 19.11)
        BigDecimal remise = BigDecimal.ZERO;
        String motifRemise = null;
        if (org != null && !courseIncluseAbonnement && org.getCoursesConsommeesMoisCourant() != null) {
            BigDecimal tauxRemise = determinerTauxRemiseVolume(org.getCoursesConsommeesMoisCourant());
            if (tauxRemise.compareTo(BigDecimal.ZERO) > 0) {
                remise = prixLivraisonBase.multiply(tauxRemise).divide(CENT, 2, RoundingMode.HALF_UP);
                motifRemise = "REMISE_VOLUME_B2B_" + tauxRemise.intValue() + "%";
            }
        }

        // 6. Total payé par l'expéditeur = livraison + service + options - remise (arrondi au 0,50 DH sup)
        BigDecimal totalBrut = prixLivraisonBase
                .add(fraisService)
                .add(suppExpress)
                .add(suppCreneau)
                .add(suppArrets)
                .add(suppAttente)
                .add(fraisRetour)
                .subtract(remise);
        BigDecimal totalPayeExpediteur = arrondirAuDemiSuperieur(totalBrut);

        // 7. Répartition économique : Gain livreur (70%) vs Commission O'liv (30%) (Section 9.5 & 19.7)
        BigDecimal basePartageable = prixLivraisonBase.add(suppAttente).add(suppArrets).add(fraisRetour);
        BigDecimal gainNetLivreur = basePartageable
                .multiply(regle.getPourcentageGainLivreur())
                .divide(CENT, 2, RoundingMode.HALF_UP);

        BigDecimal commissionOliv = prixLivraisonBase.subtract(
                prixLivraisonBase.multiply(regle.getPourcentageGainLivreur()).divide(CENT, 2, RoundingMode.HALF_UP)
        ).subtract(remise);

        // Chiffre d'affaires propre d'O'liv = Commission + Frais de service + Options plateforme (Section 10.1 & 19.8)
        BigDecimal revenuPropreOliv = commissionOliv.add(fraisService).add(suppExpress).add(suppCreneau);
        BigDecimal reserveSupport = regle.getReserveSupportPaiementParCourse();
        BigDecimal margeContributive = revenuPropreOliv.subtract(reserveSupport);

        DetailFinancierCommande detail = DetailFinancierCommande.builder()
                .commande(commande)
                .regleTarifaireAppliquee(regle)
                .versionRegleTexte(regle.getCodeVersion()) // Règle d'or n°4
                .prixLivraisonBaseDh(prixLivraisonBase)
                .fraisServiceDh(fraisService)
                .supplementExpressDh(suppExpress.add(suppCreneau))
                .supplementAttenteDh(suppAttente)
                .supplementArretsDh(suppArrets)
                .fraisEchecOuRetourDh(fraisRetour)
                .remiseAppliqueeDh(remise)
                .codePromoOuMotifRemise(motifRemise)
                .totalPayeExpediteurDh(totalPayeExpediteur)
                .gainNetLivreurDh(gainNetLivreur)
                .commissionOlivSurLivraisonDh(commissionOliv)
                .revenuPropreOlivDh(revenuPropreOliv)
                .coutsVariablesReserveDh(reserveSupport)
                .margeContributiveDh(margeContributive)
                .modePaiement(modePaiement)
                .statutPaiement(StatutPaiement.A_FACTURER)
                .montantCodAEncaisserMarchandiseDh(
                        montantCodMarchandiseDh != null ? montantCodMarchandiseDh : BigDecimal.ZERO)
                .montantCodReverseExpediteur(false)
                .build();

        commande.setDetailFinancier(detail);
        return detailFinancierRepository.save(detail);
    }

    /**
     * Applique la grille validée jusqu'à 8 km et la formule P(8km) + (D-8)*X au-delà (Section 19.4).
     */
    private BigDecimal calculerPrixBaseSelonDistance(BigDecimal d, RegleTarifaire r) {
        if (r.isUtiliserGrilleJusqua8Km()) {
            if (d.compareTo(DEUX) <= 0) return r.getPrixTranche0a2Km();
            if (d.compareTo(QUATRE) <= 0) return r.getPrixTranche2a4Km();
            if (d.compareTo(SIX) <= 0) return r.getPrixTranche4a6Km();
            if (d.compareTo(HUIT) <= 0) return r.getPrixTranche6a8Km();

            // Au-delà de 8 km : P = P(8 km) + (D - 8) * X
            BigDecimal kmAuDelaDe8 = d.subtract(HUIT);
            return r.getPrixTranche6a8Km().add(kmAuDelaDe8.multiply(r.getCoeffKmSupplementaire()));
        } else {
            // Formule continue (Section 19.3) : P = P_min + max(0, D - 2) * X
            BigDecimal kmSupp = d.subtract(r.getDistanceIncluseKm()).max(BigDecimal.ZERO);
            return r.getPrixTranche0a2Km().add(kmSupp.multiply(r.getCoeffKmSupplementaire()));
        }
    }

    private BigDecimal determinerTauxRemiseVolume(int volumeMensuel) {
        if (volumeMensuel >= 100) return new BigDecimal("15.00");
        if (volumeMensuel >= 50) return new BigDecimal("10.00");
        if (volumeMensuel >= 20) return new BigDecimal("5.00");
        return BigDecimal.ZERO;
    }

    /**
     * Règle d'or n°7 : Toute modification de prix après validation conserve ancien prix, nouveau prix, motif et auteur.
     */
    @Transactional
    public DetailFinancierCommande ajusterPrixApresValidation(
            Commande commande,
            BigDecimal nouveauTotalClientDh,
            BigDecimal fraisEchecSuppDh,
            Utilisateur auteur,
            String motif,
            String ip
    ) {
        DetailFinancierCommande detail = commande.getDetailFinancier();
        String ancienEtatJson = String.format("{\"totalPayeExpediteurDh\":%s,\"fraisEchecOuRetourDh\":%s}",
                detail.getTotalPayeExpediteurDh(), detail.getFraisEchecOuRetourDh());

        detail.setFraisEchecOuRetourDh(detail.getFraisEchecOuRetourDh().add(fraisEchecSuppDh));
        detail.setTotalPayeExpediteurDh(arrondirAuDemiSuperieur(nouveauTotalClientDh));

        String nouvelEtatJson = String.format("{\"totalPayeExpediteurDh\":%s,\"fraisEchecOuRetourDh\":%s}",
                detail.getTotalPayeExpediteurDh(), detail.getFraisEchecOuRetourDh());

        auditService.enregistrerActionSensible(
                auteur,
                "MODIFICATION_PRIX_APRES_VALIDATION",
                "Commande",
                commande.getId().toString(),
                ancienEtatJson,
                nouvelEtatJson,
                motif,
                ip
        );

        return detailFinancierRepository.save(detail);
    }
}