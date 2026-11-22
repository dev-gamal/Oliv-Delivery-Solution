package ma.skaii.oliv.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.skaii.oliv.domain.entity.RegleTarifaire;
import ma.skaii.oliv.domain.enums.ModePaiement;
import ma.skaii.oliv.domain.enums.RoleUtilisateur;
import ma.skaii.oliv.domain.enums.TypeDemande;
import ma.skaii.oliv.domain.enums.TypeVehicule;
import ma.skaii.oliv.dto.auth.RegisterRequest;
import ma.skaii.oliv.dto.commande.CommandeCreateRequest;
import ma.skaii.oliv.repository.RegleTarifaireRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OlivApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired private RegleTarifaireRepository regleTarifaireRepository;

    @BeforeEach
    void seedTarifEco() {
        if (regleTarifaireRepository.findByZoneAndVehiculeAndActifTrue("ZONE_PILOTE_MAROC", TypeVehicule.ECO).isEmpty()) {
            regleTarifaireRepository.save(RegleTarifaire.builder()
                    .codeVersion("2026-10-v3.0")
                    .zone("ZONE_PILOTE_MAROC")
                    .vehicule(TypeVehicule.ECO)
                    .prixTranche0a2Km(new BigDecimal("10.00"))
                    .prixTranche2a4Km(new BigDecimal("14.00"))
                    .prixTranche4a6Km(new BigDecimal("18.00"))
                    .prixTranche6a8Km(new BigDecimal("23.00"))
                    .distanceIncluseKm(new BigDecimal("2.00"))
                    .coeffKmSupplementaire(new BigDecimal("2.00"))
                    .utiliserGrilleJusqua8Km(true)
                    .fraisServiceStandard(new BigDecimal("3.00"))
                    .fraisServiceExpress(new BigDecimal("5.00"))
                    .supplementOptionExpress(new BigDecimal("5.00"))
                    .supplementCreneauGaranti(new BigDecimal("5.00"))
                    .tarifAttentePar10Min(new BigDecimal("5.00"))
                    .supplementArretSimple(new BigDecimal("5.00"))
                    .pourcentageGainLivreur(new BigDecimal("70.00"))
                    .pourcentageCommissionOliv(new BigDecimal("30.00"))
                    .reserveSupportPaiementParCourse(new BigDecimal("1.00"))
                    .valideDepuis(OffsetDateTime.now().minusDays(1))
                    .actif(true)
                    .build());
        }
    }

    @Test
    @DisplayName("Parcours E2E : Inscription, Création commande valide, Cloisonnement Expéditeur et Suivi public")
    void parcoursExpediteur_Et_CloisonnementAcces() throws Exception {
        // 1. Inscription de deux expéditeurs distincts
        String tokenExp1 = registerAndGetToken("Expéditeur Un", "0611111111", "exp1@oliv.ma");
        String tokenExp2 = registerAndGetToken("Expéditeur Deux", "0622222222", "exp2@oliv.ma");

        // 2. Création d'une demande valide par Expéditeur 1 (ECO 1.8 km -> 13.00 DH)
        CommandeCreateRequest req = new CommandeCreateRequest(
                TypeDemande.COLIS, TypeVehicule.ECO, "ZONE_PILOTE_MAROC",
                "Bd Mohammed V, Beni Mellal", 32.3373, -6.3498,
                "Gamal", "0611111111", "Sonner", null,
                "Quartier Al Qods, Beni Mellal", 32.3420, -6.3600,
                "Youssef", "0633333333", "Appart 4",
                "Livre technique", true, 0.8, "20x15x5", new BigDecimal("150.00"),
                new BigDecimal("1.80"), false, false, null, null, 0, false,
                ModePaiement.PREPAIEMENT_EN_LIGNE, BigDecimal.ZERO
        );

        MvcResult createResult = mockMvc.perform(post("/api/expediteur/commandes")
                        .header("Authorization", "Bearer " + tokenExp1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceUnique").isNotEmpty())
                .andExpect(jsonPath("$.detailFinancier.totalPayeExpediteurDh").value(13.00))
                .andExpect(jsonPath("$.detailFinancier.fraisServiceDh").value(3.00))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String commandeId = createdJson.get("id").asText();
        String tokenSuivi = createdJson.get("tokenSuiviDestinataire").asText();

        // 3. Règle n°8 : Expéditeur 2 ne doit JAMAIS pouvoir consulter la commande d'Expéditeur 1
        mockMvc.perform(get("/api/expediteur/commandes/{id}", commandeId)
                        .header("Authorization", "Bearer " + tokenExp2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Règle n°8")));

        // 4. Suivi destinataire sans compte (route publique via token UUID)
        mockMvc.perform(get("/api/public/suivi/{token}", tokenSuivi))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceUnique").value(createdJson.get("referenceUnique").asText()))
                .andExpect(jsonPath("$.livreurLatitudeActuelle").doesNotExist());
    }

    @Test
    @DisplayName("Validation : Empêcher une demande incomplète (400 VALIDATION_ERROR)")
    void creerCommande_InComplete_Retourne400() throws Exception {
        String token = registerAndGetToken("Expéditeur Test", "0644444444", "test@oliv.ma");

        mockMvc.perform(post("/api/expediteur/commandes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.adresseCollecte").exists());
    }

    private String registerAndGetToken(String nom, String tel, String email) throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                nom, tel, email, "Password123!", RoleUtilisateur.EXPEDITEUR_PARTICULIER, true
        );
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}