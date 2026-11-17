package ma.skaii.oliv.mapper;

import ma.skaii.oliv.domain.entity.FactureClient;
import ma.skaii.oliv.domain.entity.RegleTarifaire;
import ma.skaii.oliv.domain.entity.VersementLivreur;
import ma.skaii.oliv.dto.finance.*;
import ma.skaii.oliv.service.FinanceReportingService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FinanceMapper {

    RegleTarifaire toEntity(RegleTarifaireRequest request);

    RegleTarifaireResponse toRegleResponse(RegleTarifaire regle);

    @Mapping(target = "livreurId", source = "livreur.id")
    @Mapping(target = "livreurNom", source = "livreur.utilisateur.nomComplet")
    VersementLivreurResponse toVersementResponse(VersementLivreur versement);

    @Mapping(target = "clientNomOuRaisonSociale", expression = "java(resoudreClientFacture(facture))")
    FactureClientResponse toFactureResponse(FactureClient facture);

    SyntheseFinanciereKpiResponse toKpiResponse(FinanceReportingService.SyntheseFinanciereKpi kpi);

    default String resoudreClientFacture(FactureClient facture) {
        if (facture.getOrganisationPro() != null) {
            return facture.getOrganisationPro().getRaisonSociale();
        }
        return facture.getExpediteurParticulier() != null
                ? facture.getExpediteurParticulier().getNomComplet()
                : "Client";
    }
}