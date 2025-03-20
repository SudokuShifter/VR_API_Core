package com.inlinegroup.vrcalculationbackend.api.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class PredictResponseDto {
    @JsonProperty("Расход по газу")
    Double gasConsumption;
    @JsonProperty("mape")
    Double gasError;
    Object error;
}
