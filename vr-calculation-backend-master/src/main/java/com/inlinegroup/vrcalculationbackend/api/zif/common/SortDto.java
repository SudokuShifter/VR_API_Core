package com.inlinegroup.vrcalculationbackend.api.zif.common;

import lombok.Builder;

import java.io.Serializable;
import java.util.List;

@Builder
public record SortDto(
        List<OrderDto> orders
) implements Serializable {
}
