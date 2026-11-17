package ma.skaii.oliv.mapper;

import ma.skaii.oliv.domain.entity.ProfilLivreur;
import ma.skaii.oliv.dto.dispatch.LivreurCandidatResponse;
import ma.skaii.oliv.dto.livreur.OnboardingLivreurRequest;
import ma.skaii.oliv.dto.livreur.ProfilLivreurResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LivreurMapper {

    @Mapping(target = "utilisateurId", source = "utilisateur.id")
    @Mapping(target = "nomComplet", source = "utilisateur.nomComplet")
    @Mapping(target = "telephone", source = "utilisateur.telephone")
    ProfilLivreurResponse toResponse(ProfilLivreur profilLivreur);

    ProfilLivreur toEntity(OnboardingLivreurRequest request);

    @Mapping(target = "profilLivreurId", source = "livreur.id")
    @Mapping(target = "nomComplet", source = "livreur.utilisateur.nomComplet")
    @Mapping(target = "distanceApprocheKm", source = "distanceApprocheKm")
    LivreurCandidatResponse toCandidatResponse(ProfilLivreur livreur, Double distanceApprocheKm);
}