package ma.skaii.oliv.service;

import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.entity.HistoriqueStatut;
import ma.skaii.oliv.domain.entity.Preuve;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.domain.enums.EtapePreuve;
import ma.skaii.oliv.domain.enums.StatutCommande;
import ma.skaii.oliv.repository.CommandeRepository;
import ma.skaii.oliv.repository.HistoriqueStatutRepository;
import ma.skaii.oliv.repository.PreuveRepository;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandeStateMachineServiceTest {

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private HistoriqueStatutRepository historiqueStatutRepository;

    @Mock
    private PreuveRepository preuveRepository;

    @InjectMocks
    private CommandeStateMachineService stateMachineService;

    @Test
    @DisplayName("Transition autorisée BROUILLON -> CONFIRMEE : historisée et sauvegardée")
    void changerStatut_TransitionValide_EnregistreHistorique() {
        Commande cmd = Commande.builder().id(UUID.randomUUID()).statut(StatutCommande.BROUILLON).build();
        Utilisateur auteur = Utilisateur.builder().id(UUID.randomUUID()).nomComplet("Gamal").build();

        when(commandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Commande res = stateMachineService.changerStatut(cmd, StatutCommande.CONFIRMEE, auteur, "Validation client");

        assertThat(res.getStatut()).isEqualTo(StatutCommande.CONFIRMEE);
        assertThat(res.getDateConfirmation()).isNotNull();
        verify(historiqueStatutRepository, times(1)).save(any(HistoriqueStatut.class));
    }

    @Test
    @DisplayName("Règle n°5 : Passage à LIVREE sans preuve enregistrée doit être refusé")
    void changerStatut_LivreeSansPreuve_LeveException() {
        UUID cmdId = UUID.randomUUID();
        Commande cmd = Commande.builder().id(cmdId).statut(StatutCommande.EN_LIVRAISON).build();

        when(preuveRepository.findByCommandeIdAndEtape(cmdId, EtapePreuve.LIVRAISON))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> stateMachineService.changerStatut(
                cmd, StatutCommande.LIVREE, new Utilisateur(), "Remise"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Règle n°5");

        verify(commandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Règle n°5 : Passage à LIVREE avec preuve valide est accepté")
    void changerStatut_LivreeAvecPreuve_Reussit() {
        UUID cmdId = UUID.randomUUID();
        Commande cmd = Commande.builder().id(cmdId).statut(StatutCommande.EN_LIVRAISON).build();

        when(preuveRepository.findByCommandeIdAndEtape(cmdId, EtapePreuve.LIVRAISON))
                .thenReturn(Optional.of(new Preuve()));
        when(commandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Commande res = stateMachineService.changerStatut(cmd, StatutCommande.LIVREE, new Utilisateur(), "PIN OK");

        assertThat(res.getStatut()).isEqualTo(StatutCommande.LIVREE);
        assertThat(res.getDateLivraisonEffective()).isNotNull();
    }

    @Test
    @DisplayName("Règle n°6 : Impossible de clôturer deux fois une commande CLOTUREE")
    void changerStatut_CommandeDejaCloturee_LeveException() {
        Commande cmd = Commande.builder().id(UUID.randomUUID()).statut(StatutCommande.CLOTUREE).build();

        assertThatThrownBy(() -> stateMachineService.changerStatut(
                cmd, StatutCommande.CLOTUREE, new Utilisateur(), "Double clôture"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Règle n°6");
    }
}