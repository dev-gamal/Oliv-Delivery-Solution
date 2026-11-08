package ma.skaii.oliv.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.skaii.oliv.domain.enums.ModePaiement;
import ma.skaii.oliv.domain.enums.StatutPaiement;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "details_financiers_commandes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetailFinancierCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false, unique = true)
    private Commande commande;

    // Règle n°4 : Version exacte de la règle tarifaire et commission figée à la confirmation
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regle_tarifaire_id", nullable = false)
    private RegleTarifaire regleTarifaireAppliquee;

    @Column(nullable = false)
    private String versionRegleTexte; // Ex: "2026-10-v3.0"

    // Décomposition transparente du prix client (Règle n°2 & n°3, Section 9.8)
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal prixLivraisonBaseDh; // P

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal fraisServiceDh; // Ligne distincte obligatoire (Règle n°3)

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal supplementExpressDh = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal supplementAttenteDh = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal supplementArretsDh = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal fraisEchecOuRetourDh = BigDecimal.ZERO; // Règle n°9 : Coûts d'échec jamais masqués

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal remiseAppliqueeDh = BigDecimal.ZERO;

    private String codePromoOuMotifRemise;

    // Total = livraison + service + options - remise (Arrondi au 0,50 DH supérieur - Section 19.13)
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPayeExpediteurDh;

    // Répartition économique & Marge contributive (Section 9.5 & 10.2 & 10.3)
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal gainNetLivreurDh; // Fonds de tiers (Ex: 70% de P + part options)

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal commissionOlivSurLivraisonDh; // Ex: 30% de P

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal revenuPropreOlivDh; // Commission + Frais de service (Chiffre d'affaires O'liv)

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal coutsVariablesReserveDh; // Ex: 1.00 DH (Support, paiement)

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal margeContributiveDh; // Revenu propre O'liv - coûts variables

    // Gestion du paiement et des fonds de tiers (COD) - Section 10.1
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModePaiement modePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statutPaiement;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal montantCodAEncaisserMarchandiseDh = BigDecimal.ZERO; // Argent encaissé pour le compte de l'expéditeur

    private boolean montantCodReverseExpediteur;
    private String referenceRapprochementPaiement;
}