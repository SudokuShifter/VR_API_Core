package com.inlinegroup.vrcalculationbackend.api.zif.common;

import lombok.Builder;

import java.io.Serializable;

@Builder
public record PageableDto(
        Boolean isPaged,
        Integer pageNumber,
        Integer pageSize,
        Integer offset,
        SortDto sort,
        Boolean hasPrevious
) implements Serializable {
}
