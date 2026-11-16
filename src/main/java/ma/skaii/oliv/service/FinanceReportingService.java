package ma.skaii.oliv.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.entity.DetailFinancierCommande;
import ma.skaii.oliv.domain.entity.ProfilLivreur;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.domain.entity.VersementLivreur;
import ma.skaii.oliv.domain.enums.StatutCommande;
import ma.skaii.oliv.repository.CommandeRepository;
import ma.skaii.oliv.repository.ProfilLivreurRepository;
import ma.skaii.oliv.repository.VersementLivreurRepository;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinanceReportingService {

    private final VersementLivreurRepository versementLivreurRepository;
    private final ProfilLivreurRepository profilLivreurRepository;
    private final CommandeRepository commandeRepository;
    private final AuditService auditService;

    /**
     * Section 9.5 & 10.1 : Génère et exécute un versement du solde dû au livreur (Fonds de tiers).
     */
    @Transactional
    public VersementLivreur executerVersementLivreur(
            UUID profilLivreurId,
            LocalDate debut,
            LocalDate fin,
            BigDecimal primesEventuellesDh,
            String modeVersement,
            Utilisateur agentFinance,
            String ip
    ) {
        ProfilLivreur livreur = profilLivreurRepository.findById(profilLivreurId)
                .orElseThrow(() -> new BusinessRuleException("Livreur introuvable."));

        BigDecimal soldeActuel = livreur.getSoldeAPercevoirDh();
        BigDecimal primes = primesEventuellesDh != null ? primesEventuellesDh : BigDecimal.ZERO;

        if (soldeActuel.compareTo(BigDecimal.ZERO) <= 0 && primes.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Aucun solde positif à verser pour ce livreur.");
        }

        BigDecimal totalVerse = soldeActuel.add(primes);
        String refPaiement = "VIR-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        VersementLivreur versement = VersementLivreur.builder()
                .livreur(livreur)
                .referencePaiement(refPaiement)
                .periodeDebut(debut)
                .periodeFin(fin)
                .montantGainsCoursesDh(soldeActuel)
                .montantPrimesDh(primes)
                .montantTotalVerseDh(totalVerse)
                .statutVersement("VIRE")
                .modeVersement(modeVersement)
                .dateExecution(OffsetDateTime.now())
                .build();

        // Remise à zéro du solde à percevoir après versement
        livreur.setSoldeAPercevoirDh(BigDecimal.ZERO);
        profilLivreurRepository.save(livreur);

        auditService.enregistrerActionSensible(
                agentFinance, "VERSEMENT_GAINS_LIVREUR", "ProfilLivreur",
                livreur.getId().toString(), soldeActuel.toString(), "0.00",
                "Versement périodique réf " + refPaiement, ip
        );

        return versementLivreurRepository.save(versement);
    }

    /**
     * Section 10.3 & 19.12 : Calcul des formules financières et du seuil de rentabilité.
     */
    @Transactional(readOnly = true)
    public SyntheseFinanciereKpi calculerSyntheseFinanciere(BigDecimal chargesFixesMensuellesDh) {
        List<Commande> commandesCloturees = commandeRepository.findAll().stream()
                .filter(c -> c.getStatut() == StatutCommande.CLOTUREE && c.getDetailFinancier() != null)
                .toList();

        int nbCourses = commandesCloturees.size();
        BigDecimal volumeTotalPayeClients = BigDecimal.ZERO;
        BigDecimal fondsTiersGainsLivreurs = BigDecimal.ZERO;
        BigDecimal chiffreAffairesPropreOliv = BigDecimal.ZERO;
        BigDecimal margeContributiveTotale = BigDecimal.ZERO;

        for (Commande c : commandesCloturees) {
            DetailFinancierCommande df = c.getDetailFinancier();
            volumeTotalPayeClients = volumeTotalPayeClients.add(df.getTotalPayeExpediteurDh());
            fondsTiersGainsLivreurs = fondsTiersGainsLivreurs.add(df.getGainNetLivreurDh());
            chiffreAffairesPropreOliv = chiffreAffairesPropreOliv.add(df.getRevenuPropreOlivDh());
            margeContributiveTotale = margeContributiveTotale.add(df.getMargeContributiveDh());
        }

        BigDecimal margeMoyenneParCourse = nbCourses > 0
                ? margeContributiveTotale.divide(BigDecimal.valueOf(nbCourses), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Seuil de rentabilité (courses) = charges fixes mensuelles / marge contributive moyenne par course (Section 10.3)
        int seuilRentabiliteCourses = (margeMoyenneParCourse.compareTo(BigDecimal.ZERO) > 0 && chargesFixesMensuellesDh != null)
                ? chargesFixesMensuellesDh.divide(margeMoyenneParCourse, 0, RoundingMode.CEILING).intValue()
                : 0;

        return SyntheseFinanciereKpi.builder()
                .nombreCoursesCloturees(nbCourses)
                .volumeTotalClientsDh(volumeTotalPayeClients)
                .fondsTiersLivreursDh(fondsTiersGainsLivreurs)
                .chiffreAffairesPropreOlivDh(chiffreAffairesPropreOliv)
                .margeContributiveTotaleDh(margeContributiveTotale)
                .margeMoyenneParCourseDh(margeMoyenneParCourse)
                .seuilRentabiliteEnCourses(seuilRentabiliteCourses)
                .build();
    }

    @Getter
    @Builder
    public static class SyntheseFinanciereKpi {
        private final int nombreCoursesCloturees;
        private final BigDecimal volumeTotalClientsDh;
        private final BigDecimal fondsTiersLivreursDh;
        private final BigDecimal chiffreAffairesPropreOlivDh;
        private final BigDecimal margeContributiveTotaleDh;
        private final BigDecimal margeMoyenneParCourseDh;
        private final int seuilRentabiliteEnCourses;
    }
}