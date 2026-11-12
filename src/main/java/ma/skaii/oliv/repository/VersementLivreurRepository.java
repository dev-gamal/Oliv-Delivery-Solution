package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.VersementLivreur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersementLivreurRepository extends JpaRepository<VersementLivreur, UUID> {
    Optional<VersementLivreur> findByReferencePaiement(String referencePaiement);
    List<VersementLivreur> findByLivreurId(UUID livreurId);
}