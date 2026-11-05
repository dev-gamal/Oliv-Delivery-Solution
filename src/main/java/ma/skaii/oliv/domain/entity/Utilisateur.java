package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.skaii.oliv.domain.enums.RoleUtilisateur;
import ma.skaii.oliv.domain.enums.StatutCompte;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "utilisateurs", indexes = {
        @Index(name = "idx_user_tel", columnList = "telephone", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nomComplet;

    @Column(nullable = false, unique = true, length = 20)
    private String telephone;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String motDePasseHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleUtilisateur role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCompte statut;

    private boolean telephoneVerifie;
    private boolean emailVerifie;

    // Rattachement optionnel si l'utilisateur fait partie d'une entreprise/restaurant (Section 3)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private OrganisationExpediteur organisation;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = OffsetDateTime.now();
        if (this.statut == null) this.statut = StatutCompte.EN_ATTENTE_VERIFICATION;
    }
}