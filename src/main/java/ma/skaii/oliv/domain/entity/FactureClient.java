package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.skaii.oliv.domain.enums.StatutPaiement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "factures_clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FactureClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String numeroFacture; // Ex: FAC-2026-0001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private OrganisationExpediteur organisationPro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediteur_particulier_id")
    private Utilisateur expediteurParticulier;

    @Column(nullable = false)
    private LocalDate dateEmission;

    private LocalDate dateEcheance;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montantHtDh;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montantTvaDh;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montantTtcDh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statut;

    private String modeReglement;
    private String referenceRapprochementBancaire;
    private String fichierPdfUrl;
    private OffsetDateTime dateReglementEffectif;
}