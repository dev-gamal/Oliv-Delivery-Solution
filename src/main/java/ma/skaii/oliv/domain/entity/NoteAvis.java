package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notes_avis")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoteAvis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    // Notation mutuelle Expéditeur <-> Livreur (Section 4)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Utilisateur auteur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cible_id", nullable = false)
    private Utilisateur cible;

    @Column(nullable = false)
    private int noteSurCinq; // 1 à 5

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreation;
}