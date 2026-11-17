package ma.skaii.oliv.mapper;

import ma.skaii.oliv.domain.entity.Commande;
import ma.skaii.oliv.domain.entity.DetailFinancierCommande;
import ma.skaii.oliv.domain.entity.HistoriqueStatut;
import ma.skaii.oliv.domain.enums.StatutCommande;
import ma.skaii.oliv.dto.commande.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommandeMapper {

    Commande toEntity(CommandeCreateRequest request);

    @Mapping(target = "versionRegleTarifaire", source = "versionRegleTexte")
    DetailFinancierClientResponse toDetailClientResponse(DetailFinancierCommande detail);

    @Mapping(target = "detailClient", source = ".")
    DetailFinancierAdminResponse toDetailAdminResponse(DetailFinancierCommande detail);

    @Mapping(target = "auteurNom", source = "auteur.nomComplet")
    HistoriqueStatutResponse toHistoriqueResponse(HistoriqueStatut historique);

    List<HistoriqueStatutResponse> toHistoriqueResponseList(List<HistoriqueStatut> historiques);

    @Mapping(target = "livreurPrenom", source = "livreurAffecte.utilisateur.nomComplet", qualifiedByName = "extrairePrenom")
    @Mapping(target = "detailFinancier", source = "detailFinancier")
    CommandeExpediteurResponse toExpediteurResponse(Commande commande);

    @Mapping(target = "expediteurId", source = "expediteur.id")
    @Mapping(target = "expediteurNom", source = "expediteur.nomComplet")
    @Mapping(target = "organisationId", source = "organisationPro.id")
    @Mapping(target = "livreurAffecteId", source = "livreurAffecte.id")
    @Mapping(target = "livreurNom", source = "livreurAffecte.utilisateur.nomComplet")
    @Mapping(target = "detailFinancierAdmin", source = "detailFinancier")
    CommandeAdminFinanceResponse toAdminFinanceResponse(Commande commande);

    @Mapping(target = "expediteurNomOuEnseigne", expression = "java(resoudreNomEnseigne(commande))")
    @Mapping(target = "livreurPrenom", source = "livreurAffecte.utilisateur.nomComplet", qualifiedByName = "extrairePrenom")
    @Mapping(target = "vehicule", source = "vehiculeRequis")
    @Mapping(target = "livreurLatitudeActuelle", expression = "java(estEnLivraison(commande) && commande.getLivreurAffecte() != null ? commande.getLivreurAffecte().getDerniereLatitude() : null)")
    @Mapping(target = "livreurLongitudeActuelle", expression = "java(estEnLivraison(commande) && commande.getLivreurAffecte() != null ? commande.getLivreurAffecte().getDerniereLongitude() : null)")
    @Mapping(target = "dateDernierePosition", expression = "java(estEnLivraison(commande) && commande.getLivreurAffecte() != null ? commande.getLivreurAffecte().getDateDernierePosition() : null)")
    @Mapping(target = "etapes", source = "historiqueStatuts")
    SuiviDestinatairePublicResponse toSuiviPublicResponse(Commande commande);

    @Named("extrairePrenom")
    default String extrairePrenom(String nomComplet) {
        if (nomComplet == null || nomComplet.isBlank()) return null;
        return nomComplet.trim().split("\\s+")[0];
    }

    default String resoudreNomEnseigne(Commande commande) {
        if (commande.getOrganisationPro() != null) {
            return commande.getOrganisationPro().getRaisonSociale();
        }
        return commande.getExpediteur() != null ? commande.getExpediteur().getNomComplet() : "Expéditeur O'liv";
    }

    default boolean estEnLivraison(Commande commande) {
        return commande.getStatut() == StatutCommande.EN_LIVRAISON;
    }
}