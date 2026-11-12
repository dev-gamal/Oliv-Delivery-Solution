package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.OrganisationExpediteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganisationExpediteurRepository extends JpaRepository<OrganisationExpediteur, UUID> {
    Optional<OrganisationExpediteur> findByIce(String ice);
    Optional<OrganisationExpediteur> findByRaisonSocialeContainingIgnoreCase(String raisonSociale);
}