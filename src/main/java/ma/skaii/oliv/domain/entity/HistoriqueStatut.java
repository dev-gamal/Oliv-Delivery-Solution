package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.skaii.oliv.domain.enums.StatutCommande;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "historiques_statuts", indexes = {
        @Index(name = "idx_hist_commande", columnList = "commande_id, horodatage")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistoriqueStatut {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false, updatable = false)
    private Commande commande;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private StatutCommande ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private StatutCommande nouveauStatut;

    // Règle n°1 : Historique horodaté non modifiable
    @Column(nullable = false, updatable = false)
    private OffsetDateTime horodatage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", updatable = false)
    private Utilisateur auteur;

    @Column(updatable = false)
    private String motifOuCommentaire;
}