package com.inlinegroup.vrcalculationbackend.api.zif.om;

import com.inlinegroup.vrcalculationbackend.api.zif.common.PageableDto;
import com.inlinegroup.vrcalculationbackend.api.zif.common.SortDto;
import lombok.Builder;

import java.util.List;

@Builder
public record OmPropertyPageDto(
        List<OmPropertyDto> content,
        PageableDto pageable,
        Integer number,
        Integer size,
        Integer numberOfElements,
        SortDto sort,
        Boolean isFirst,
        Boolean hasPrevious,
        Integer total,
        Boolean hasNext,
        Boolean isLast,
        Integer totalPages,
        Integer totalElements,
        Boolean last,
        Boolean first,
        Boolean empty
) {
}
