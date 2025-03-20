package com.inlinegroup.vrcalculationbackend.api.zif.common;

import lombok.Builder;

@Builder
public record ProblemDetailsDto(
        String type,
        String title,
        Integer status,
        String detail,
        String instance,
        String additionalProp1,
        String additionalProp2,
        String additionalProp3
) {
}
