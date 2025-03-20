package com.inlinegroup.vrcalculationbackend.api.fmm;

import lombok.Builder;

import java.util.List;

@Builder
public record DetailErrorDto(
        List<Object> loc,
        String msg,
        String type
) {
}
