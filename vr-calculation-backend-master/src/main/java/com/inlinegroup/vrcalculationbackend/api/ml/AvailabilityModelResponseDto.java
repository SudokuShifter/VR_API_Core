package com.inlinegroup.vrcalculationbackend.api.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record AvailabilityModelResponseDto(
        @JsonProperty("model_id") Integer modelId,
        @JsonProperty("model_status") Boolean modelStatus
) {
}
