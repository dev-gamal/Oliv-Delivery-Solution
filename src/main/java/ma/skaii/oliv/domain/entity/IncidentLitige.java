package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.skaii.oliv.domain.enums.MotifIncident;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "incidents_litiges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidentLitige {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "declarant_id", nullable = false)
    private Utilisateur declarant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotifIncident motif; // Motif normalisé obligatoire (Section 6.1 & 15)

    @Column(columnDefinition = "TEXT")
    private String description;

    private String pieceJointePhotoUrl;

    // Décision de l'opérateur : NOUVELLE_TENTATIVE, RETOUR_EXPEDITEUR, ANNULATION, CLOTURE
    private String decisionOperateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operateur_id")
    private Utilisateur operateurTraitant;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montantRemboursementClientDh = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal indemniteTempsEngageLivreurDh = BigDecimal.ZERO; // Section 6.1

    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateDeclaration;

    private OffsetDateTime dateResolution;
}