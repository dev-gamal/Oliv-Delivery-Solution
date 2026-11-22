package ma.skaii.oliv.service;

import ma.skaii.oliv.domain.entity.*;
import ma.skaii.oliv.domain.enums.ModePaiement;
import ma.skaii.oliv.domain.enums.TypeVehicule;
import ma.skaii.oliv.repository.DetailFinancierCommandeRepository;
import ma.skaii.oliv.repository.RegleTarifaireRepository;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingEngineServiceTest {

    @Mock
    private RegleTarifaireRepository regleTarifaireRepository;

    @Mock
    private DetailFinancierCommandeRepository detailFinancierRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PricingEngineService pricingEngineService;

    private RegleTarifaire regleEco;

    @BeforeEach
    void setUp() {
        regleEco = RegleTarifaire.builder()
                .id(UUID.randomUUID())
                .codeVersion("2026-10-v3.0")
                .zone("ZONE_PILOTE_MAROC")
                .vehicule(TypeVehicule.ECO)
                .prixTranche0a2Km(new BigDecimal("10.00"))
                .prixTranche2a4Km(new BigDecimal("14.00"))
                .prixTranche4a6Km(new BigDecimal("18.00"))
                .prixTranche6a8Km(new BigDecimal("23.00"))
                .distanceIncluseKm(new BigDecimal("2.00"))
                .coeffKmSupplementaire(new BigDecimal("2.00"))
                .utiliserGrilleJusqua8Km(true)
                .fraisServiceStandard(new BigDecimal("3.00"))
                .fraisServiceExpress(new BigDecimal("5.00"))
                .supplementOptionExpress(new BigDecimal("5.00"))
                .supplementCreneauGaranti(new BigDecimal("5.00"))
                .tarifAttentePar10Min(new BigDecimal("5.00"))
                .supplementArretSimple(new BigDecimal("5.00"))
                .pourcentageGainLivreur(new BigDecimal("70.00"))
                .pourcentageCommissionOliv(new BigDecimal("30.00"))
                .reserveSupportPaiementParCourse(new BigDecimal("1.00"))
                .actif(true)
                .build();
    }

    @Test
    @DisplayName("ECO 0-2 km : Total 13 DH, Gain livreur 7 DH, Revenu O'liv 6 DH, Marge 5 DH")
    void calculerTarif_EcoCourteDistance_ConformeAnnexe19() {
        Commande cmd = Commande.builder()
                .zone("ZONE_PILOTE_MAROC")
                .vehiculeRequis(TypeVehicule.ECO)
                .distanceItineraireKm(new BigDecimal("1.60")) // Arrondie à 2.00 km
                .build();

        when(regleTarifaireRepository.findByZoneAndVehiculeAndActifTrue("ZONE_PILOTE_MAROC", TypeVehicule.ECO))
                .thenReturn(Optional.of(regleEco));
        when(detailFinancierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DetailFinancierCommande detail = pricingEngineService.calculerEtFigerTarif(
                cmd, ModePaiement.PREPAIEMENT_EN_LIGNE, BigDecimal.ZERO
        );

        assertThat(cmd.getDistanceFactureeKm()).isEqualByComparingTo("2.00");
        assertThat(detail.getVersionRegleTexte()).isEqualTo("2026-10-v3.0");
        assertThat(detail.getPrixLivraisonBaseDh()).isEqualByComparingTo("10.00");
        assertThat(detail.getFraisServiceDh()).isEqualByComparingTo("3.00");
        assertThat(detail.getTotalPayeExpediteurDh()).isEqualByComparingTo("13.00");
        assertThat(detail.getGainNetLivreurDh()).isEqualByComparingTo("7.00");
        assertThat(detail.getCommissionOlivSurLivraisonDh()).isEqualByComparingTo("3.00");
        assertThat(detail.getRevenuPropreOlivDh()).isEqualByComparingTo("6.00");
        assertThat(detail.getMargeContributiveDh()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("ECO > 8 km (10 km) : P = 23 + (10 - 8) * 2 = 27 DH, Total client = 30 DH")
    void calculerTarif_EcoAuDela8Km_AppliqueFormuleKilometrique() {
        Commande cmd = Commande.builder()
                .zone("ZONE_PILOTE_MAROC")
                .vehiculeRequis(TypeVehicule.ECO)
                .distanceItineraireKm(new BigDecimal("9.80")) // Arrondie à 10.00 km
                .build();

        when(regleTarifaireRepository.findByZoneAndVehiculeAndActifTrue("ZONE_PILOTE_MAROC", TypeVehicule.ECO))
                .thenReturn(Optional.of(regleEco));
        when(detailFinancierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DetailFinancierCommande detail = pricingEngineService.calculerEtFigerTarif(
                cmd, ModePaiement.PREPAIEMENT_EN_LIGNE, BigDecimal.ZERO
        );

        assertThat(cmd.getDistanceFactureeKm()).isEqualByComparingTo("10.00");
        assertThat(detail.getPrixLivraisonBaseDh()).isEqualByComparingTo("27.00");
        assertThat(detail.getTotalPayeExpediteurDh()).isEqualByComparingTo("30.00");
        assertThat(detail.getGainNetLivreurDh()).isEqualByComparingTo("18.90");
        assertThat(detail.getMargeContributiveDh()).isEqualByComparingTo("10.10");
    }

    @Test
    @DisplayName("Remise volume B2B (25 courses) : -5% sur livraison ECO 2 km -> Total 12.50 DH, Marge 4.50 DH")
    void calculerTarif_RemiseVolumeB2B_Applique5Pourcent() {
        OrganisationExpediteur orgPro = OrganisationExpediteur.builder()
                .raisonSociale("Resto Atlas")
                .coursesConsommeesMoisCourant(25)
                .build();

        Commande cmd = Commande.builder()
                .zone("ZONE_PILOTE_MAROC")
                .vehiculeRequis(TypeVehicule.ECO)
                .distanceItineraireKm(new BigDecimal("1.50"))
                .organisationPro(orgPro)
                .build();

        when(regleTarifaireRepository.findByZoneAndVehiculeAndActifTrue("ZONE_PILOTE_MAROC", TypeVehicule.ECO))
                .thenReturn(Optional.of(regleEco));
        when(detailFinancierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DetailFinancierCommande detail = pricingEngineService.calculerEtFigerTarif(
                cmd, ModePaiement.FACTURATION_MENSUELLE_PRO, BigDecimal.ZERO
        );

        assertThat(detail.getRemiseAppliqueeDh()).isEqualByComparingTo("0.50");
        assertThat(detail.getTotalPayeExpediteurDh()).isEqualByComparingTo("12.50");
        assertThat(detail.getMargeContributiveDh()).isEqualByComparingTo("4.50");
    }

    @Test
    @DisplayName("Véhicule PRO Utilitaire : doit bloquer le calcul automatique (sur devis)")
    void calculerTarif_VehiculePro_LeveException() {
        Commande cmd = Commande.builder()
                .zone("ZONE_PILOTE_MAROC")
                .vehiculeRequis(TypeVehicule.PRO)
                .distanceItineraireKm(new BigDecimal("5.00"))
                .build();

        assertThatThrownBy(() -> pricingEngineService.calculerEtFigerTarif(
                cmd, ModePaiement.FACTURATION_MENSUELLE_PRO, BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("devis");
    }
}