package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.AcceptationCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcceptationConditionRepository extends JpaRepository<AcceptationCondition, UUID> {
    List<AcceptationCondition> findByUtilisateurId(UUID utilisateurId);
}