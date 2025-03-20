package com.inlinegroup.vrcalculationbackend.api.fmm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FMMSolutionDto(
        @JsonProperty("q_gas") Double qGas,
        @JsonProperty("q_gc") Double qGc,
        @JsonProperty("q_wat") Double qWat,
        @JsonProperty("error_gas") Double errorGas,
        @JsonProperty("error_gc") Double errorGc,
        @JsonProperty("q_wat_error") Double errorWat
) {
}
