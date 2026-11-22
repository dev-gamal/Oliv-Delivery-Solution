package ma.skaii.oliv.service;

import ma.skaii.oliv.domain.entity.*;
import ma.skaii.oliv.domain.enums.*;
import ma.skaii.oliv.repository.CommandeRepository;
import ma.skaii.oliv.repository.OffreCourseRepository;
import ma.skaii.oliv.repository.ProfilLivreurRepository;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingDispatchServiceTest {

    @Mock private ProfilLivreurRepository profilLivreurRepository;
    @Mock private OffreCourseRepository offreCourseRepository;
    @Mock private CommandeRepository commandeRepository;
    @Mock private CommandeStateMachineService stateMachineService;

    @InjectMocks
    private MatchingDispatchService matchingDispatchService;

    @Test
    @DisplayName("Onboarding livreur : aucune offre ne peut être envoyée à un livreur non VERIFIE")
    void envoyerOffre_LivreurNonVerifie_LeveException() {
        UUID cmdId = UUID.randomUUID();
        UUID livId = UUID.randomUUID();

        Commande cmd = Commande.builder().id(cmdId).vehiculeRequis(TypeVehicule.MOTO).build();
        Utilisateur uLiv = Utilisateur.builder().statut(StatutCompte.ACTIF).build();
        ProfilLivreur livreurNonVerifie = ProfilLivreur.builder()
                .id(livId)
                .utilisateur(uLiv)
                .vehiculeDeclare(TypeVehicule.MOTO)
                .statutVerification(StatutVerificationLivreur.CANDIDAT)
                .build();

        when(commandeRepository.findById(cmdId)).thenReturn(Optional.of(cmd));
        when(profilLivreurRepository.findById(livId)).thenReturn(Optional.of(livreurNonVerifie));

        assertThatThrownBy(() -> matchingDispatchService.envoyerOffreManuelle(cmdId, livId, new Utilisateur()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("non vérifié");

        verify(offreCourseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Refus d'offre : journalise REFUSEE et remet la commande en EN_RECHERCHE_LIVREUR")
    void repondreAOffre_Refus_RemetCommandeEnRecherche() {
        UUID offreId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Utilisateur livreurUser = Utilisateur.builder().id(userId).build();
        ProfilLivreur profil = ProfilLivreur.builder().utilisateur(livreurUser).build();
        Commande cmd = Commande.builder().id(UUID.randomUUID()).statut(StatutCommande.OFFRE_ENVOYEE).build();

        OffreCourse offre = OffreCourse.builder()
                .id(offreId)
                .commande(cmd)
                .livreur(profil)
                .statutReponse(StatutOffre.ENVOYEE)
                .heureExpiration(OffsetDateTime.now().plusSeconds(45))
                .build();

        when(offreCourseRepository.findById(offreId)).thenReturn(Optional.of(offre));
        when(offreCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OffreCourse res = matchingDispatchService.repondreAOffre(offreId, livreurUser, false, "Trop loin");

        assertThat(res.getStatutReponse()).isEqualTo(StatutOffre.REFUSEE);
        assertThat(res.getMotifRefus()).isEqualTo("Trop loin");
        verify(stateMachineService).changerStatut(eq(cmd), eq(StatutCommande.EN_RECHERCHE_LIVREUR), eq(livreurUser), anyString());
    }

    @Test
    @DisplayName("Expiration d'offre : passe en EXPIREE et remet la commande en EN_RECHERCHE_LIVREUR")
    void repondreAOffre_Expiree_RemetCommandeEnRecherche() {
        UUID offreId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Utilisateur livreurUser = Utilisateur.builder().id(userId).build();
        ProfilLivreur profil = ProfilLivreur.builder().utilisateur(livreurUser).build();
        Commande cmd = Commande.builder().id(UUID.randomUUID()).statut(StatutCommande.OFFRE_ENVOYEE).build();

        OffreCourse offre = OffreCourse.builder()
                .id(offreId)
                .commande(cmd)
                .livreur(profil)
                .statutReponse(StatutOffre.ENVOYEE)
                .heureExpiration(OffsetDateTime.now().minusSeconds(10)) // Déjà expirée
                .build();

        when(offreCourseRepository.findById(offreId)).thenReturn(Optional.of(offre));

        assertThatThrownBy(() -> matchingDispatchService.repondreAOffre(offreId, livreurUser, true, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expiré");

        assertThat(offre.getStatutReponse()).isEqualTo(StatutOffre.EXPIREE);
        verify(stateMachineService).changerStatut(eq(cmd), eq(StatutCommande.EN_RECHERCHE_LIVREUR), eq(livreurUser), anyString());
    }
}