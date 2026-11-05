package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.skaii.oliv.domain.enums.StatutVerificationLivreur;
import ma.skaii.oliv.domain.enums.TypeVehicule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "profils_livreurs", indexes = {
        @Index(name = "idx_livreur_dispo_zone", columnList = "enLigne, statutVerification, zoneAffectation")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfilLivreur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String zoneAffectation; // Ex: "Casablanca-Centre", "Beni Mellal"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeVehicule vehiculeDeclare;

    @Column(nullable = false)
    private boolean enLigne; // Bouton disponibilité (Section 7.2)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutVerificationLivreur statutVerification; // Aucune course sans statut VERIFIE (Section 3)

    // Pièces KYC (URLs vers stockage sécurisé S3/MinIO)
    private String pieceIdentiteUrl;
    private String permisConduireUrl;
    private String justificatifStatutAutoEntrepreneurUrl;
    private boolean formationCourteValidee;

    // Géolocalisation collectée uniquement pendant une course (Section 7.4)
    private Double derniereLatitude;
    private Double derniereLongitude;
    private OffsetDateTime dateDernierePosition;

    // Indicateurs de performance et portefeuille (Section 7.2)
    @Column(precision = 3, scale = 2)
    private BigDecimal noteMoyenne;

    @Column(precision = 5, scale = 2)
    private BigDecimal tauxAcceptation;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal soldeAPercevoirDh = BigDecimal.ZERO;
}