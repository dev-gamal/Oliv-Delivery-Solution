package ma.skaii.oliv.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.domain.enums.StatutCompte;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Utilisateur utilisateur;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()));
    }

    @Override
    public String getPassword() {
        return utilisateur.getMotDePasseHash();
    }

    @Override
    public String getUsername() {
        // Identifiant principal : téléphone (recommandé par la section 3)
        return utilisateur.getTelephone();
    }

    @Override
    public boolean isAccountNonLocked() {
        return utilisateur.getStatut() != StatutCompte.SUSPENDU;
    }

    @Override
    public boolean isEnabled() {
        return utilisateur.getStatut() != StatutCompte.DESACTIVE;
    }
}