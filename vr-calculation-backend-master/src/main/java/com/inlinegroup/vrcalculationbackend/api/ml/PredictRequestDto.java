package com.inlinegroup.vrcalculationbackend.api.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class PredictRequestDto {
    @JsonProperty("features")
    private FeaturesDto features;
    @JsonProperty("well_id")
    private Integer wellId;
    @JsonProperty("target_name")
    private String targetName;

    public PredictRequestDto() {
        this.features = FeaturesDto.builder().build();
    }
}