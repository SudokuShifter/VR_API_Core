package com.inlinegroup.vrcalculationbackend.api.fmm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTaskDto{
    @JsonProperty("q_gas_timed")
    private List<Double> qGasTimed;
    @JsonProperty("q_gc_timed")
    private List<Double> qGcTimed;
    @JsonProperty("q_wat_timed")
    private List<Double> qWatTimed;
}
