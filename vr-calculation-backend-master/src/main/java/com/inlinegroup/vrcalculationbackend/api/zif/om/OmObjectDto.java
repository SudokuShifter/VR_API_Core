package com.inlinegroup.vrcalculationbackend.api.zif.om;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inlinegroup.vrcalculationbackend.api.zif.common.PathItemDto;
import lombok.Builder;

import java.util.List;

@Builder
public record OmObjectDto(
        String id,
        Boolean isDeleted,
        @JsonProperty("parentid") String parentId,
        @JsonProperty("objectprototypeid") String objectPrototypeId,
        String name,
        String description,
        String path,
        List<String> pathIds,
        @JsonProperty("haschildren") Boolean hasChildren,
        @JsonProperty("childscount") Integer childsCount,
        @JsonProperty("modelid") String modelId,
        List<String> hierarchyScopeIds,
        String classId,
        List<String> pathNames,
        List<PathItemDto> pathItems
) {
}
