package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "acceptations_conditions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AcceptationCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String typeDocument; // Ex: "CGU_EXPEDITEUR", "CGU_LIVREUR", "CONSENTEMENT_GPS"

    @Column(nullable = false)
    private String versionDocument; // Ex: "v3.0-2026-10"

    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateAcceptation;

    private String adresseIp;
}