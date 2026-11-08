package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.skaii.oliv.domain.enums.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "commandes", indexes = {
        @Index(name = "idx_cmd_ref", columnList = "referenceUnique", unique = true),
        @Index(name = "idx_cmd_statut_zone", columnList = "statut, zone"),
        @Index(name = "idx_cmd_token_suivi", columnList = "tokenSuiviDestinataire", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Règle n°1 : Identifiant unique lisible (ex: OLIV-20261102-8F4A)
    @Column(nullable = false, unique = true, updatable = false, length = 30)
    private String referenceUnique;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDemande typeDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeVehicule vehiculeRequis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommande statut;

    @Column(nullable = false)
    private String zone;

    // Acteurs liés à la commande (Règle n°8 : Cloisonnement strict)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expediteur_id", nullable = false)
    private Utilisateur expediteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private OrganisationExpediteur organisationPro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livreur_affecte_id")
    private ProfilLivreur livreurAffecte;

    // Collecte
    @Column(nullable = false, columnDefinition = "TEXT")
    private String adresseCollecte;
    @Column(nullable = false)
    private Double latitudeCollecte;
    @Column(nullable = false)
    private Double longitudeCollecte;
    private String contactCollecteNom;
    private String contactCollecteTel;
    private String instructionsCollecte;

    // Spécificité Commande Commerçant / Restaurant (Section 5 & 6.2)
    private String referenceInterneCommercant;
    private boolean commandeSignaleePrete;
    private OffsetDateTime heureSignalementPrete;

    // Livraison & Destinataire
    @Column(nullable = false, columnDefinition = "TEXT")
    private String adresseLivraison;
    @Column(nullable = false)
    private Double latitudeLivraison;
    @Column(nullable = false)
    private Double longitudeLivraison;
    @Column(nullable = false)
    private String destinataireNom;
    @Column(nullable = false, length = 20)
    private String destinataireTelephone;
    private String instructionsLivraison;

    // Lien sécurisé sans compte obligatoire pour le destinataire (Section 3 & Règle n°8)
    @Column(nullable = false, unique = true)
    private String tokenSuiviDestinataire;
    @Column(length = 10)
    private String codePinLivraisonHash;

    // Caractéristiques du colis & Trajet
    @Column(nullable = false)
    private String natureContenu;
    private boolean declarationMarchandiseAutorisee; // Section 12.1
    private Double poidsKg;
    private String dimensionsCm;
    @Column(precision = 10, scale = 2)
    private BigDecimal valeurDeclareeDh;

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal distanceItineraireKm;
    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal distanceFactureeKm; // Arrondie au 0,5 km supérieur (Section 19.13)

    // Options choisies (Section 9.4)
    private boolean optionExpress;
    private boolean optionCreneauGaranti;
    private OffsetDateTime creneauDebut;
    private OffsetDateTime creneauFin;
    private int nombreArretsSupplementaires;
    private int minutesAttenteCollecte;
    private boolean retourPointCollecteRequis;

    // Horodatages clés du parcours (Section 6)
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateConfirmation;
    private OffsetDateTime dateAffectationLivreur;
    private OffsetDateTime dateCollecteEffective;
    private OffsetDateTime dateLivraisonEffective;
    private OffsetDateTime dateCloture; // Règle n°6 : Une fois clôturée, immuable

    // Relations enfants
    @OneToOne(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private DetailFinancierCommande detailFinancier;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("horodatage ASC")
    @Builder.Default
    private List<HistoriqueStatut> historiqueStatuts = new ArrayList<>();

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    @Builder.Default
    private List<OffreCourse> offres = new ArrayList<>();

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Preuve> preuves = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.dateCreation = OffsetDateTime.now();
        if (this.tokenSuiviDestinataire == null) {
            this.tokenSuiviDestinataire = UUID.randomUUID().toString();
        }
    }
}