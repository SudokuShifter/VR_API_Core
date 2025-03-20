package com.inlinegroup.vrcalculationbackend.api.fmm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ValidationSolutionDto(
        @JsonProperty("wct") Double wct,
        @JsonProperty("gas_condensate_factor") Double gasCondensateFactor
) {
}
