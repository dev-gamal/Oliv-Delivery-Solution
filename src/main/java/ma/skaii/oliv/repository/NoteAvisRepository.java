package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.NoteAvis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteAvisRepository extends JpaRepository<NoteAvis, UUID> {
    List<NoteAvis> findByCibleId(UUID cibleId); // Obtenir tous les avis d'un livreur ou expéditeur
    List<NoteAvis> findByCommandeId(UUID commandeId);
}