package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.FactureClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FactureClientRepository extends JpaRepository<FactureClient, UUID> {
    Optional<FactureClient> findByNumeroFacture(String numeroFacture);
    List<FactureClient> findByOrganisationProId(UUID organisationId);
}