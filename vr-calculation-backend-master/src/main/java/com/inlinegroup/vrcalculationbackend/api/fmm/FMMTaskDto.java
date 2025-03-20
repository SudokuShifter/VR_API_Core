package com.inlinegroup.vrcalculationbackend.api.fmm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inlinegroup.vrcalculationbackend.api.ml.FeaturesDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FMMTaskDto {
    @JsonProperty("gamma_gas")
    private Double gammaGas;
    @JsonProperty("gamma_gc")
    private Double gammaGc;
    @JsonProperty("gamma_wat")
    private Double gammaWat;
    @JsonProperty("d_tube")
    private Double dTube;
    @JsonProperty("d_choke_percent")
    private Double dChokePercent;
    @JsonProperty("gas_condensate_factor")
    private Double gasCondensateFactor;
    private Double wct;
    @JsonProperty("p_out")
    private Double pOut;
    @JsonProperty("p_buf")
    private Double pBuf;
    @JsonProperty("t_buf")
    private Double tBuf;
    @JsonProperty("t_out")
    private Double tOut;
    @JsonProperty("q_gc")
    private Double qGc;
    @JsonProperty("q_gas")
    private Double qGas;
    @JsonProperty("q_wat")
    private Double qWat;
    @JsonProperty("d_choke_percent_adapt")
    private List<Double> dChokePercentAdapt;
    @JsonProperty("c_choke_adapt")
    private List<Double> cChokeAdapt;
    @JsonIgnore
    private FeaturesDto dataForMLWithoutCF = FeaturesDto.builder().build();
}
