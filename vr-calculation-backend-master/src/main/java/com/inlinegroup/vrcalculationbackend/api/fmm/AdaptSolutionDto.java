package com.inlinegroup.vrcalculationbackend.api.fmm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AdaptSolutionDto(
        @JsonProperty("d_choke_percent_adapt") List<Double> dChokePercentAdapt,
        @JsonProperty("c_choke_adapt") List<Double> cChokeAdapt
) {
}
