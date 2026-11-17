package ma.skaii.oliv.security;

import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String identifiant) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByTelephone(identifiant)
                .or(() -> utilisateurRepository.findByEmail(identifiant))
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + identifiant));

        return new CustomUserDetails(utilisateur);
    }
}