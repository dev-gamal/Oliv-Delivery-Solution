package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "journal_audit", indexes = {
        @Index(name = "idx_audit_objet", columnList = "typeObjet, objetId"),
        @Index(name = "idx_audit_date", columnList = "horodatage")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, updatable = false)
    private Utilisateur auteur;

    // Ex: "MODIFICATION_PRIX_APRES_VALIDATION", "SUSPENSION_LIVREUR", "AJUSTEMENT_COMMANDE_CLOTUREE"
    @Column(nullable = false, updatable = false)
    private String action;

    @Column(nullable = false, updatable = false)
    private String typeObjet; // Ex: "Commande", "RegleTarifaire", "ProfilLivreur"

    @Column(nullable = false, updatable = false)
    private String objetId;

    // Règle n°7 : Conserve l'ancienne et la nouvelle valeur (ex: ancien prix / nouveau prix)
    @Column(columnDefinition = "TEXT", updatable = false)
    private String ancienneValeurJson;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String nouvelleValeurJson;

    @Column(nullable = false, updatable = false)
    private String motifObligatoire;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime horodatage;

    @Column(updatable = false)
    private String adresseIp;
}