package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JournalAuditRepository extends JpaRepository<JournalAudit, UUID> {
    List<JournalAudit> findByTypeObjetAndObjetIdOrderByHorodatageDesc(String typeObjet, String objetId);
}