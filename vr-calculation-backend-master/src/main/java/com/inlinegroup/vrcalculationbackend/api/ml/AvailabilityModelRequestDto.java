package com.inlinegroup.vrcalculationbackend.api.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record AvailabilityModelRequestDto(
        @JsonProperty("well_id") Integer wellId,
        @JsonProperty("target_name") String targetName
) {
}
