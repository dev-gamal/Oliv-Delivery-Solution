package ma.skaii.oliv.mapper;

import ma.skaii.oliv.domain.entity.IncidentLitige;
import ma.skaii.oliv.domain.entity.NoteAvis;
import ma.skaii.oliv.domain.entity.Preuve;
import ma.skaii.oliv.dto.incident.IncidentResponse;
import ma.skaii.oliv.dto.preuve.PreuveResponse;
import ma.skaii.oliv.dto.qualite.NoteAvisResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PreuveIncidentMapper {

    @Mapping(target = "commandeId", source = "commande.id")
    @Mapping(target = "auteurNom", source = "auteur.nomComplet")
    PreuveResponse toPreuveResponse(Preuve preuve);

    @Mapping(target = "commandeId", source = "commande.id")
    @Mapping(target = "referenceCommande", source = "commande.referenceUnique")
    @Mapping(target = "declarantNom", source = "declarant.nomComplet")
    IncidentResponse toIncidentResponse(IncidentLitige incident);

    @Mapping(target = "commandeId", source = "commande.id")
    @Mapping(target = "auteurNom", source = "auteur.nomComplet")
    @Mapping(target = "cibleNom", source = "cible.nomComplet")
    NoteAvisResponse toNoteAvisResponse(NoteAvis noteAvis);
}