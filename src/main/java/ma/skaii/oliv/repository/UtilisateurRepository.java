package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.domain.enums.RoleUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByTelephone(String telephone);
    List<Utilisateur> findByRole(RoleUtilisateur role);
}