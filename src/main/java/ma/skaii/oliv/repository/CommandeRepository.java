package ma.skaii.oliv.repository;

import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.enums.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, UUID> {
    Optional<Commande> findByReferenceUnique(String referenceUnique);

    // Pour le suivi public sans compte (Lien envoyé par SMS au destinataire)
    Optional<Commande> findByTokenSuiviDestinataire(String token);

    // Cloisonnement : l'expéditeur ne voit que ses courses
    List<Commande> findByExpediteurIdOrderByDateCreationDesc(UUID expediteurId);
    List<Commande> findByOrganisationProIdOrderByDateCreationDesc(UUID organisationId);

    // Pour le livreur : voir son historique de courses
    List<Commande> findByLivreurAffecteIdOrderByDateCreationDesc(UUID livreurId);

    // Pour l'opérateur Dispatch : filtrer par zone et statut (ex: voir les EN_RECHERCHE_LIVREUR)
    List<Commande> findByZoneAndStatutOrderByDateCreationAsc(String zone, StatutCommande statut);
}