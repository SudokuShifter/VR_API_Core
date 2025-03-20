package com.inlinegroup.vrcalculationbackend.api.zif.om;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inlinegroup.vrcalculationbackend.api.zif.common.PathItemDto;
import lombok.Builder;

import java.util.List;

@Builder
public record OmPropertyDto(
        String id,
        Boolean isDeleted,
        String dataTypeId,
        @JsonProperty("haschildren") Boolean hasChildren,
        List<String> hierarchyScopeIds,
        String name,
        String objectId,
        String path,
        List<String> pathIds,
        String propertyType,
        List<String> pathNames,
        String parentId,
        String propertyPrimitiveId,
        String uomId,
        String valueTypeId,
        List<PathItemDto> pathItems,
        String description,
        boolean descriptionOverridden
) {
}
