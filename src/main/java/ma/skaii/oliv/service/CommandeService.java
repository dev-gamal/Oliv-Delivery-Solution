package ma.skaii.oliv.service;

import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.*;
import ma.skaii.oliv.domain.enums.*;
import ma.skaii.oliv.repository.*;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final PreuveRepository preuveRepository;
    private final IncidentLitigeRepository incidentRepository;
    private final ProfilLivreurRepository profilLivreurRepository;
    private final OrganisationExpediteurRepository organisationRepository;
    private final PricingEngineService pricingEngineService;
    private final CommandeStateMachineService stateMachineService;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Étapes 1 & 2 : Création de la demande en BROUILLON et estimation transparente du prix.
     */
    @Transactional
    public Commande creerBrouillonEtEstimer(
            Commande brouillon,
            Utilisateur expediteur,
            ModePaiement modePaiement,
            BigDecimal montantCodDh
    ) {
        if (!brouillon.isDeclarationMarchandiseAutorisee()) {
            throw new BusinessRuleException("L'expéditeur doit certifier que le colis ne contient aucune marchandise interdite (Section 12.1).");
        }

        brouillon.setReferenceUnique(genererReferenceUnique());
        brouillon.setExpediteur(expediteur);
        brouillon.setOrganisationPro(expediteur.getOrganisation());
        brouillon.setStatut(StatutCommande.BROUILLON);
        brouillon.setTokenSuiviDestinataire(UUID.randomUUID().toString());

        Commande sauvegardee = commandeRepository.save(brouillon);

        // Historique initial
        stateMachineService.changerStatut(sauvegardee, StatutCommande.BROUILLON, expediteur, "Création du brouillon");
        pricingEngineService.calculerEtFigerTarif(sauvegardee, modePaiement, montantCodDh);

        return sauvegardee;
    }

    /**
     * Étape 3 : Confirmation par l'expéditeur et passage en file de recherche livreur.
     */
    @Transactional
    public Commande confirmerCommande(UUID commandeId, Utilisateur expediteur) {
        Commande commande = getCommandePourExpediteur(commandeId, expediteur);

        // Incrémenter le compteur mensuel B2B si rattaché à une organisation
        if (commande.getOrganisationPro() != null) {
            OrganisationExpediteur org = commande.getOrganisationPro();
            int actuel = org.getCoursesConsommeesMoisCourant() != null ? org.getCoursesConsommeesMoisCourant() : 0;
            org.setCoursesConsommeesMoisCourant(actuel + 1);
            organisationRepository.save(org);
        }

        stateMachineService.changerStatut(commande, StatutCommande.CONFIRMEE, expediteur, "Validation prix et conditions par l'expéditeur");
        return stateMachineService.changerStatut(commande, StatutCommande.EN_RECHERCHE_LIVREUR, expediteur, "Mise en file de dispatching");
    }

    /**
     * Section 6.2 : Cas d'usage « Commande de restaurant » -> signaler « commande prête ».
     */
    @Transactional
    public Commande signalerCommandePreteCommercant(UUID commandeId, Utilisateur expediteurPro) {
        Commande commande = getCommandePourExpediteur(commandeId, expediteurPro);
        if (commande.getTypeDemande() != TypeDemande.COMMANDE_COMMERCANT) {
            throw new BusinessRuleException("L'indicateur 'Commande prête' est réservé aux commandes commerçants/restaurants.");
        }
        commande.setCommandeSignaleePrete(true);
        commande.setHeureSignalementPrete(OffsetDateTime.now());
        return commandeRepository.save(commande);
    }

    /**
     * Étape 6 : Collecte par le livreur avec enregistrement de preuve de collecte.
     */
    @Transactional
    public Commande confirmerCollecte(
            UUID commandeId,
            Utilisateur livreurUser,
            TypePreuve typePreuve,
            String valeurOuPhotoUrl,
            Double lat,
            Double lng
    ) {
        Commande commande = getCommandePourLivreurAffecte(commandeId, livreurUser);

        Preuve preuveCollecte = Preuve.builder()
                .commande(commande)
                .etape(EtapePreuve.COLLECTE)
                .typePreuve(typePreuve)
                .valeurOuFichierUrl(valeurOuPhotoUrl)
                .auteur(livreurUser)
                .horodatage(OffsetDateTime.now())
                .latitudeCapture(lat)
                .longitudeCapture(lng)
                .build();
        preuveRepository.save(preuveCollecte);

        return stateMachineService.changerStatut(commande, StatutCommande.COLLECTEE, livreurUser, "Colis pris en charge au point de collecte");
    }

    /**
     * Étape 7 : Démarrage du transport vers le destinataire et génération du code PIN à usage unique.
     */
    @Transactional
    public String demarrerLivraisonEtGenererPin(UUID commandeId, Utilisateur livreurUser) {
        Commande commande = getCommandePourLivreurAffecte(commandeId, livreurUser);
        String codePinClair = String.format("%04d", RANDOM.nextInt(10000));
        commande.setCodePinLivraisonHash(passwordEncoder.encode(codePinClair));
        stateMachineService.changerStatut(commande, StatutCommande.EN_LIVRAISON, livreurUser, "En route vers le destinataire");
        return codePinClair; // À envoyer par SMS/WhatsApp au destinataire avec son lien de suivi
    }

    /**
     * Étapes 8 & 9 : Remise avec preuve obligatoire (Règle n°5) puis Clôture et Règlement automatique (Règle n°6).
     */
    @Transactional
    public Commande confirmerLivraisonEtCloturer(
            UUID commandeId,
            Utilisateur livreurUser,
            TypePreuve typePreuve,
            String codePinSaisiOuFichierUrl,
            Double lat,
            Double lng
    ) {
        Commande commande = getCommandePourLivreurAffecte(commandeId, livreurUser);

        // Si Document, remise en main propre avec code obligatoire (Section 5)
        if (commande.getTypeDemande() == TypeDemande.DOCUMENT && typePreuve != TypePreuve.CODE_PIN_OTP) {
            throw new BusinessRuleException("Un pli/document exige obligatoirement une remise contre code PIN à usage unique (Section 5).");
        }

        if (typePreuve == TypePreuve.CODE_PIN_OTP) {
            if (commande.getCodePinLivraisonHash() == null
                    || !passwordEncoder.matches(codePinSaisiOuFichierUrl, commande.getCodePinLivraisonHash())) {
                throw new BusinessRuleException("Code PIN de livraison invalide.");
            }
        }

        // Enregistrement de la preuve (requis avant le passage à LIVREE par la StateMachine)
        Preuve preuveLivraison = Preuve.builder()
                .commande(commande)
                .etape(EtapePreuve.LIVRAISON)
                .typePreuve(typePreuve)
                .valeurOuFichierUrl(typePreuve == TypePreuve.CODE_PIN_OTP ? "PIN_OTP_VALIDE" : codePinSaisiOuFichierUrl)
                .auteur(livreurUser)
                .horodatage(OffsetDateTime.now())
                .latitudeCapture(lat)
                .longitudeCapture(lng)
                .build();
        preuveRepository.save(preuveLivraison);

        // Étape 8 : Passage à LIVREE
        stateMachineService.changerStatut(commande, StatutCommande.LIVREE, livreurUser, "Remise confirmée avec preuve");

        // Étape 9 : Clôture et crédit du portefeuille livreur (Section 6 & 7.2)
        ProfilLivreur livreur = commande.getLivreurAffecte();
        BigDecimal gainCourse = commande.getDetailFinancier().getGainNetLivreurDh();
        livreur.setSoldeAPercevoirDh(livreur.getSoldeAPercevoirDh().add(gainCourse));
        profilLivreurRepository.save(livreur);

        return stateMachineService.changerStatut(commande, StatutCommande.CLOTUREE, livreurUser,
                "Clôture automatique et crédit du gain livreur (" + gainCourse + " DH)");
    }

    /**
     * Section 6.1 : Parcours d'échec (destinataire absent, adresse incorrecte, colis refusé...).
     */
    @Transactional
    public IncidentLitige declarerEchecOuIncident(
            UUID commandeId,
            Utilisateur declarant,
            MotifIncident motif,
            String description,
            String photoUrl
    ) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new BusinessRuleException("Commande introuvable."));

        IncidentLitige incident = IncidentLitige.builder()
                .commande(commande)
                .declarant(declarant)
                .motif(motif)
                .description(description)
                .pieceJointePhotoUrl(photoUrl)
                .dateDeclaration(OffsetDateTime.now())
                .build();
        incidentRepository.save(incident);

        stateMachineService.changerStatut(commande, StatutCommande.ECHEC_INCIDENT, declarant,
                "Incident déclaré : " + motif.name());
        return incident;
    }

    /**
     * Règle n°8 : Cloisonnement strict pour l'expéditeur (ou son organisation Pro).
     */
    @Transactional(readOnly = true)
    public Commande getCommandePourExpediteur(UUID commandeId, Utilisateur expediteur) {
        Commande cmd = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new BusinessRuleException("Commande introuvable."));

        boolean memeUtilisateur = cmd.getExpediteur().getId().equals(expediteur.getId());
        boolean memeOrganisation = expediteur.getOrganisation() != null
                && cmd.getOrganisationPro() != null
                && cmd.getOrganisationPro().getId().equals(expediteur.getOrganisation().getId());

        if (!memeUtilisateur && !memeOrganisation) {
            throw new BusinessRuleException("Règle n°8 : Accès refusé. Vous ne pouvez consulter que vos propres commandes.");
        }
        return cmd;
    }

    @Transactional(readOnly = true)
    public List<Commande> listerMesCommandesExpediteur(Utilisateur expediteur) {
        if (expediteur.getOrganisation() != null) {
            return commandeRepository.findByOrganisationProIdOrderByDateCreationDesc(expediteur.getOrganisation().getId());
        }
        return commandeRepository.findByExpediteurIdOrderByDateCreationDesc(expediteur.getId());
    }

    private Commande getCommandePourLivreurAffecte(UUID commandeId, Utilisateur livreurUser) {
        Commande cmd = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new BusinessRuleException("Commande introuvable."));
        if (cmd.getLivreurAffecte() == null
                || !cmd.getLivreurAffecte().getUtilisateur().getId().equals(livreurUser.getId())) {
            throw new BusinessRuleException("Règle n°8 : Accès refusé. Cette course ne vous est pas affectée.");
        }
        return cmd;
    }

    private String genererReferenceUnique() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomHex = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "OLIV-" + datePart + "-" + randomHex;
    }
}