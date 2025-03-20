package com.inlinegroup.vrcalculationbackend.api.fmm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdaptTaskDto {
    @JsonProperty("gamma_gas")
    private Double gammaGas;
    @JsonProperty("gamma_gc")
    private Double gammaGc;
    @JsonProperty("gamma_wat")
    private Double gammaWat;
    @JsonProperty("d_tube")
    private Double dTube;
    @JsonProperty("d_choke_percent_timed")
    private List<Double> dChokePercentTimed;
    @JsonProperty("p_buf_timed")
    private List<Double> pBufTimed;
    @JsonProperty("p_out_timed")
    private List<Double> pOutTimed;
    @JsonProperty("t_buf_timed")
    private List<Double> tBufTimed;
    @JsonProperty("t_out_timed")
    private List<Double> tOutTimed;
    @JsonProperty("q_gc_timed")
    private List<Double> qGcTimed;
    @JsonProperty("q_gas_timed")
    private List<Double> qGasTimed;
    @JsonProperty("q_wat_timed")
    private List<Double> qWatTimed;
    private List<String> timestamp;
}
