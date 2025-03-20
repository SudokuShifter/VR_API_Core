package com.inlinegroup.vrcalculationbackend.api.zif.common;

import lombok.Builder;

import java.io.Serializable;

@Builder
public record PathItemDto(
        String id,
        String name
) implements Serializable {
}
