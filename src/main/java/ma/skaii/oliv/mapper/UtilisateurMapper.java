package ma.skaii.oliv.mapper;

import ma.skaii.oliv.domain.entity.OrganisationExpediteur;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.dto.user.OrganisationProRequest;
import ma.skaii.oliv.dto.user.OrganisationProResponse;
import ma.skaii.oliv.dto.user.UtilisateurResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UtilisateurMapper {

    @Mapping(target = "organisationId", source = "organisation.id")
    UtilisateurResponse toResponse(Utilisateur utilisateur);

    OrganisationExpediteur toEntity(OrganisationProRequest request);

    OrganisationProResponse toOrganisationResponse(OrganisationExpediteur organisation);
}