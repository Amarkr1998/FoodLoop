package com.foodloop.impact.api;

import com.foodloop.impact.domain.ImpactSummary;

public record PersonalImpactResponse(ImpactSummaryResponse asDonor, ImpactSummaryResponse asReceiver) {

    public static PersonalImpactResponse of(ImpactSummary asDonor, ImpactSummary asReceiver) {
        return new PersonalImpactResponse(ImpactSummaryResponse.from(asDonor), ImpactSummaryResponse.from(asReceiver));
    }
}
