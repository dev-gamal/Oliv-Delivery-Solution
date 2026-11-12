package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.RegleTarifaire;
import ma.skaii.oliv.domain.enums.TypeVehicule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegleTarifaireRepository extends JpaRepository<RegleTarifaire, UUID> {
    // Récupère la grille tarifaire active pour une zone et un véhicule donné
    Optional<RegleTarifaire> findByZoneAndVehiculeAndActifTrue(String zone, TypeVehicule vehicule);
    Optional<RegleTarifaire> findByCodeVersionAndZoneAndVehicule(String codeVersion, String zone, TypeVehicule vehicule);
}