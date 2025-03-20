package com.inlinegroup.vrcalculationbackend.api.fmm;

import lombok.Builder;

import java.util.List;

@Builder
public record HttpValidationErrorDto(
        List<DetailErrorDto> detail
) {
}
