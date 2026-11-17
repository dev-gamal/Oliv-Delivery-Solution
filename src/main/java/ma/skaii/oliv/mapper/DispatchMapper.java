package ma.skaii.oliv.mapper;

import ma.skaii.oliv.domain.entity.OffreCourse;
import ma.skaii.oliv.dto.dispatch.OffreCourseLivreurResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DispatchMapper {

    @Mapping(target = "offreId", source = "id")
    @Mapping(target = "commandeId", source = "commande.id")
    @Mapping(target = "referenceCommande", source = "commande.referenceUnique")
    @Mapping(target = "adresseCollecte", source = "commande.adresseCollecte")
    @Mapping(target = "adresseLivraison", source = "commande.adresseLivraison")
    @Mapping(target = "distanceLivraisonKm", source = "commande.distanceFactureeKm")
    OffreCourseLivreurResponse toLivreurResponse(OffreCourse offre);

    List<OffreCourseLivreurResponse> toLivreurResponseList(List<OffreCourse> offres);
}