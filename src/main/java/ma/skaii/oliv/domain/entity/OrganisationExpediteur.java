package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "organisations_expediteurs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganisationExpediteur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String raisonSociale;

    @Column(length = 50)
    private String ice; // Identifiant Commun de l'Entreprise (Maroc)

    private String nomContactPrincipal;
    private String telephoneContact;
    private String emailFacturation;

    @Column(columnDefinition = "TEXT")
    private String adresseFacturation;

    @Column(columnDefinition = "TEXT")
    private String adressesCollecteFavoritesJson; // Stockage des adresses fréquentes

    // Plan d'abonnement actif (START_20, BUSINESS_50, PRO_100, MIX_100) - Section 9.6
    private String planAbonnementActif;
    private BigDecimal prixMensuelAbonnement;
    private Integer quotaCoursesInclusesMensuel;
    private Integer coursesConsommeesMoisCourant;
    private BigDecimal fraisServiceSousContrat; // 0 à 2 DH selon contrat (Section 9.3 / 19.10)
    private LocalDate dateRenouvellementAbonnement;

    @OneToMany(mappedBy = "organisation", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Utilisateur> utilisateursRattaches = new ArrayList<>();
}