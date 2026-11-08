package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.skaii.oliv.domain.enums.ModeDispatch;
import ma.skaii.oliv.domain.enums.StatutOffre;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "offres_courses", indexes = {
        @Index(name = "idx_offre_cmd_livreur", columnList = "commande_id, livreur_id, statutReponse")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OffreCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livreur_id", nullable = false)
    private ProfilLivreur livreur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModeDispatch modeDispatch; // MANUEL_ASSISTE (MVP) ou AUTOMATIQUE (Phase 2)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operateur_id")
    private Utilisateur operateurDispatch; // Renseigné si mode manuel assisté

    // Informations affichées au livreur avant acceptation (Section 6 & 7.2)
    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal distanceApprocheCollecteKm;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal gainNetEstimeAfficheDh;

    @Column(nullable = false)
    private OffsetDateTime heureEnvoi;

    @Column(nullable = false)
    private int delaiReponseSecondes; // Configurable (Section 6)

    @Column(nullable = false)
    private OffsetDateTime heureExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutOffre statutReponse;

    private OffsetDateTime heureReponse;
    private String motifRefus;
}