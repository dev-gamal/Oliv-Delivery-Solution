package ma.skaii.oliv.dto.finance;

import java.math.BigDecimal;

public record SyntheseFinanciereKpiResponse(
        int nombreCoursesCloturees,
        BigDecimal volumeTotalClientsDh,
        BigDecimal fondsTiersLivreursDh,
        BigDecimal chiffreAffairesPropreOlivDh,
        BigDecimal margeContributiveTotaleDh,
        BigDecimal margeMoyenneParCourseDh,
        int seuilRentabiliteEnCourses
) {}