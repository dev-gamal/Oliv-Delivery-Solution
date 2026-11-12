package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.DetailFinancierCommande;
import ma.skaii.oliv.domain.enums.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DetailFinancierCommandeRepository extends JpaRepository<DetailFinancierCommande, UUID> {
    Optional<DetailFinancierCommande> findByCommandeId(UUID commandeId);
    List<DetailFinancierCommande> findByStatutPaiement(StatutPaiement statutPaiement);
}