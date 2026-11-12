package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.Preuve;
import ma.skaii.oliv.domain.enums.EtapePreuve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreuveRepository extends JpaRepository<Preuve, UUID> {
    List<Preuve> findByCommandeId(UUID commandeId);
    Optional<Preuve> findByCommandeIdAndEtape(UUID commandeId, EtapePreuve etape);
}