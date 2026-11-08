package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.skaii.oliv.domain.enums.EtapePreuve;
import ma.skaii.oliv.domain.enums.TypePreuve;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "preuves_livraison")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Preuve {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false, updatable = false)
    private Commande commande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EtapePreuve etape; // COLLECTE ou LIVRAISON (Section 7.4)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TypePreuve typePreuve; // CODE_PIN_OTP, SIGNATURE, PHOTO, DEROGATION_TRACEE

    // Code validé ou URL chiffrée vers le fichier (photo/signature)
    @Column(nullable = false, updatable = false)
    private String valeurOuFichierUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id", nullable = false, updatable = false)
    private Utilisateur auteur;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime horodatage;

    private Double latitudeCapture;
    private Double longitudeCapture;
}