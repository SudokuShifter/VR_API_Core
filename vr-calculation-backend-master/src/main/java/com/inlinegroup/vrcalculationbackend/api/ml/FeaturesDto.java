package com.inlinegroup.vrcalculationbackend.api.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class FeaturesDto {
    @JsonProperty("Давление")
    private Double pOut;
    @JsonProperty("Давление забойное")
    private Double pDownHole;
    @JsonProperty("Давление над буферной задвижкой ФА")
    private Double pBuf;
    @JsonProperty("Процент открытия штуцера")
    private Double dChokePercent;
    @JsonProperty("Температура на выкидной линии")
    private Double tBuf;
    @JsonProperty("Температура")
    private Double tOut;
    @JsonProperty("Температура забойная")
    private Double tDownHole;
    @JsonProperty("Расход по газу Вентури")
    private Double qGas;
}