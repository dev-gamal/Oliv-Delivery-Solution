package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.skaii.oliv.domain.enums.TypeVehicule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "regles_tarifaires", indexes = {
        @Index(name = "idx_tarif_version", columnList = "codeVersion, zone, vehicule")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegleTarifaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String codeVersion; // Ex: "2026-10-v3.0" (Section 9.8)

    @Column(nullable = false)
    private String zone; // Ex: "CASABLANCA"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeVehicule vehicule;

    // Grille par tranches jusqu'à 8 km (Section 9.2 & 19.4)
    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal prixTranche0a2Km; // P_min (ECO: 10, MOTO: 15, AUTO: 25)

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal prixTranche2a4Km; // ECO: 14, MOTO: 19, AUTO: 30

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal prixTranche4a6Km; // ECO: 18, MOTO: 24, AUTO: 40

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal prixTranche6a8Km; // ECO: 23, MOTO: 30, AUTO: 50

    // Paramètres kilométriques au-delà de 8 km ou formule continue (Section 19.2)
    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal distanceIncluseKm; // Par défaut 2.00 km

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal coeffKmSupplementaire; // X (ECO: 2.00, MOTO: 2.50, AUTO: 5.00)

    private boolean utiliserGrilleJusqua8Km; // Recommandation Section 19.4

    // Frais de service et Options (Section 9.3 & 9.4)
    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal fraisServiceStandard; // 3.00 DH

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal fraisServiceExpress; // 5.00 DH

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal supplementOptionExpress; // 5.00 à 10.00 DH

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal supplementCreneauGaranti; // 5.00 DH

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal tarifAttentePar10Min; // 5.00 DH après 10 min de franchise

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal supplementArretSimple; // 5.00 DH

    // Répartition Livreur / Commission O'liv (Section 9.5 & 19.7)
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal pourcentageGainLivreur; // Ex: 70.00 (%)

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal pourcentageCommissionOliv; // Ex: 30.00 (%)

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal reserveSupportPaiementParCourse; // Ex: 1.00 DH

    @Column(nullable = false)
    private OffsetDateTime valideDepuis;

    private OffsetDateTime valideJusqua;

    @Column(nullable = false)
    private boolean actif;
}