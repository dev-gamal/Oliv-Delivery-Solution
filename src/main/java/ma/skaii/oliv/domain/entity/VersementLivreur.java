package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "versements_livreurs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VersementLivreur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livreur_id", nullable = false)
    private ProfilLivreur livreur;

    @Column(nullable = false, unique = true)
    private String referencePaiement;

    @Column(nullable = false)
    private LocalDate periodeDebut;

    @Column(nullable = false)
    private LocalDate periodeFin;

    private int nombreCoursesIncluses;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montantGainsCoursesDh;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal montantPrimesDh = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montantTotalVerseDh;

    @Column(nullable = false)
    private String statutVersement; // EN_ATTENTE, VIRE, ECHEC

    private String modeVersement; // VIREMENT_BANCAIRE, ESPECES_AGENCE
    private OffsetDateTime dateExecution;
}