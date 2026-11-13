package ma.skaii.oliv.service;

import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.entity.HistoriqueStatut;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.domain.enums.EtapePreuve;
import ma.skaii.oliv.domain.enums.StatutCommande;
import ma.skaii.oliv.repository.CommandeRepository;
import ma.skaii.oliv.repository.HistoriqueStatutRepository;
import ma.skaii.oliv.repository.PreuveRepository;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommandeStateMachineService {

    private final CommandeRepository commandeRepository;
    private final HistoriqueStatutRepository historiqueStatutRepository;
    private final PreuveRepository preuveRepository;

    private static final Map<StatutCommande, Set<StatutCommande>> TRANSITIONS_AUTORISEES = new EnumMap<>(StatutCommande.class);

    static {
        // Table exacte des transitions autorisées (Section 8 du Cahier des charges)
        TRANSITIONS_AUTORISEES.put(StatutCommande.BROUILLON,
                EnumSet.of(StatutCommande.CONFIRMEE, StatutCommande.ANNULEE));

        TRANSITIONS_AUTORISEES.put(StatutCommande.CONFIRMEE,
                EnumSet.of(StatutCommande.EN_RECHERCHE_LIVREUR, StatutCommande.ANNULEE));

        TRANSITIONS_AUTORISEES.put(StatutCommande.EN_RECHERCHE_LIVREUR,
                EnumSet.of(StatutCommande.OFFRE_ENVOYEE, StatutCommande.ANNULEE));

        TRANSITIONS_AUTORISEES.put(StatutCommande.OFFRE_ENVOYEE,
                EnumSet.of(StatutCommande.ACCEPTEE, StatutCommande.EN_RECHERCHE_LIVREUR));

        TRANSITIONS_AUTORISEES.put(StatutCommande.ACCEPTEE,
                EnumSet.of(StatutCommande.EN_ROUTE_COLLECTE, StatutCommande.ECHEC_INCIDENT, StatutCommande.ANNULEE));

        TRANSITIONS_AUTORISEES.put(StatutCommande.EN_ROUTE_COLLECTE,
                EnumSet.of(StatutCommande.COLLECTEE, StatutCommande.ECHEC_INCIDENT));

        TRANSITIONS_AUTORISEES.put(StatutCommande.COLLECTEE,
                EnumSet.of(StatutCommande.EN_LIVRAISON, StatutCommande.ECHEC_INCIDENT));

        TRANSITIONS_AUTORISEES.put(StatutCommande.EN_LIVRAISON,
                EnumSet.of(StatutCommande.LIVREE, StatutCommande.ECHEC_INCIDENT));

        TRANSITIONS_AUTORISEES.put(StatutCommande.LIVREE,
                EnumSet.of(StatutCommande.CLOTUREE));

        TRANSITIONS_AUTORISEES.put(StatutCommande.ECHEC_INCIDENT,
                EnumSet.of(StatutCommande.EN_LIVRAISON, StatutCommande.EN_RECHERCHE_LIVREUR, StatutCommande.ANNULEE, StatutCommande.CLOTUREE));

        // Règle n°6 : Une commande clôturée ou annulée ne change plus d'état
        TRANSITIONS_AUTORISEES.put(StatutCommande.CLOTUREE, EnumSet.noneOf(StatutCommande.class));
        TRANSITIONS_AUTORISEES.put(StatutCommande.ANNULEE, EnumSet.noneOf(StatutCommande.class));
    }

    @Transactional
    public Commande changerStatut(Commande commande, StatutCommande nouveauStatut, Utilisateur auteur, String motif) {
        StatutCommande statutActuel = commande.getStatut();

        // Règle n°6 : Interdiction absolue de modifier ou reclôturer une commande CLOTUREE
        if (statutActuel == StatutCommande.CLOTUREE) {
            throw new BusinessRuleException("Règle n°6 : Une commande clôturée est immuable (pas de double clôture).");
        }

        Set<StatutCommande> ciblesPermises = TRANSITIONS_AUTORISEES.getOrDefault(statutActuel, EnumSet.noneOf(StatutCommande.class));
        if (!ciblesPermises.contains(nouveauStatut)) {
            throw new BusinessRuleException(String.format(
                    "Transition de statut interdite : impossible de passer de %s à %s.", statutActuel, nouveauStatut));
        }

        // Règle n°5 : Une commande n'est pas « LIVREE » sans preuve enregistrée
        if (nouveauStatut == StatutCommande.LIVREE) {
            boolean preuvePresente = preuveRepository
                    .findByCommandeIdAndEtape(commande.getId(), EtapePreuve.LIVRAISON)
                    .isPresent();
            if (!preuvePresente) {
                throw new BusinessRuleException("Règle n°5 : Impossible de passer au statut LIVREE sans preuve (code PIN, signature, photo ou dérogation tracée).");
            }
            commande.setDateLivraisonEffective(OffsetDateTime.now());
        }

        OffsetDateTime maintenant = OffsetDateTime.now();
        if (nouveauStatut == StatutCommande.CONFIRMEE) commande.setDateConfirmation(maintenant);
        if (nouveauStatut == StatutCommande.ACCEPTEE) commande.setDateAffectationLivreur(maintenant);
        if (nouveauStatut == StatutCommande.COLLECTEE) commande.setDateCollecteEffective(maintenant);
        if (nouveauStatut == StatutCommande.CLOTUREE) commande.setDateCloture(maintenant);

        commande.setStatut(nouveauStatut);

        // Règle n°1 : Journalisation immuable dans l'historique de statut
        HistoriqueStatut historique = HistoriqueStatut.builder()
                .commande(commande)
                .ancienStatut(statutActuel)
                .nouveauStatut(nouveauStatut)
                .horodatage(maintenant)
                .auteur(auteur)
                .motifOuCommentaire(motif)
                .build();

        historiqueStatutRepository.save(historique);
        return commandeRepository.save(commande);
    }
}