package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.OffreCourse;
import ma.skaii.oliv.domain.enums.StatutOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OffreCourseRepository extends JpaRepository<OffreCourse, UUID> {
    List<OffreCourse> findByCommandeId(UUID commandeId);
    List<OffreCourse> findByLivreurIdAndStatutReponse(UUID livreurId, StatutOffre statut);

    // Vérifier si un livreur a déjà reçu une offre pour cette course (pour ne pas spammer)
    Optional<OffreCourse> findByCommandeIdAndLivreurId(UUID commandeId, UUID livreurId);
}