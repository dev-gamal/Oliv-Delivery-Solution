package ma.skaii.oliv.service;

import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.AcceptationCondition;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.domain.enums.RoleUtilisateur;
import ma.skaii.oliv.domain.enums.StatutCompte;
import ma.skaii.oliv.dto.auth.AuthResponse;
import ma.skaii.oliv.dto.auth.LoginRequest;
import ma.skaii.oliv.dto.auth.RegisterRequest;
import ma.skaii.oliv.repository.AcceptationConditionRepository;
import ma.skaii.oliv.repository.UtilisateurRepository;
import ma.skaii.oliv.security.CustomUserDetails;
import ma.skaii.oliv.security.JwtService;
import ma.skaii.oliv.service.exception.BusinessRuleException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final AcceptationConditionRepository acceptationConditionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request, String adresseIp) {
        if (request.role() == RoleUtilisateur.ADMIN
                || request.role() == RoleUtilisateur.FINANCE
                || request.role() == RoleUtilisateur.OPERATEUR_DISPATCH) {
            throw new BusinessRuleException("Inscription publique interdite pour les rôles internes.");
        }

        if (utilisateurRepository.findByTelephone(request.telephone()).isPresent()) {
            throw new BusinessRuleException("Ce numéro de téléphone est déjà utilisé.");
        }
        if (request.email() != null && utilisateurRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessRuleException("Cette adresse e-mail est déjà utilisée.");
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .nomComplet(request.nomComplet())
                .telephone(request.telephone())
                .email(request.email())
                .motDePasseHash(passwordEncoder.encode(request.motDePasse()))
                .role(request.role())
                .statut(StatutCompte.ACTIF)
                .build();

        Utilisateur saved = utilisateurRepository.save(utilisateur);

        // Traçabilité juridique de l'acceptation des CGU (Section 12.1 & 14.2)
        String typeCgu = request.role() == RoleUtilisateur.LIVREUR ? "CGU_LIVREUR" : "CGU_EXPEDITEUR";
        acceptationConditionRepository.save(AcceptationCondition.builder()
                .utilisateur(saved)
                .typeDocument(typeCgu)
                .versionDocument("v3.0-2026-10")
                .dateAcceptation(OffsetDateTime.now())
                .adresseIp(adresseIp)
                .build());

        String token = jwtService.generateToken(saved);
        return new AuthResponse(token, "Bearer", saved.getId(), saved.getNomComplet(), saved.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identifiant(), request.motDePasse())
        );

        Utilisateur utilisateur = ((CustomUserDetails) auth.getPrincipal()).getUtilisateur();
        String token = jwtService.generateToken(utilisateur);
        return new AuthResponse(token, "Bearer", utilisateur.getId(), utilisateur.getNomComplet(), utilisateur.getRole());
    }
}