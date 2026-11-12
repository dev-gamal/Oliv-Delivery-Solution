package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.ProfilLivreur;
import ma.skaii.oliv.domain.enums.StatutVerificationLivreur;
import ma.skaii.oliv.domain.enums.TypeVehicule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfilLivreurRepository extends JpaRepository<ProfilLivreur, UUID> {
    Optional<ProfilLivreur> findByUtilisateurId(UUID utilisateurId);

    // Requête cruciale pour le Matching Assisté (Livreurs dispo, vérifiés, dans la bonne zone)
    List<ProfilLivreur> findByEnLigneTrueAndStatutVerificationAndZoneAffectationAndVehiculeDeclare(
            StatutVerificationLivreur statut,
            String zoneAffectation,
            TypeVehicule vehicule
    );
}