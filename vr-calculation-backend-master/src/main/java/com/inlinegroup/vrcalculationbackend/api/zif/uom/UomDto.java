package com.inlinegroup.vrcalculationbackend.api.zif.uom;

import lombok.Builder;

@Builder
public record UomDto(
        String id,
        Boolean isDeleted,
        String uomClassId,
        String name,
        String abbreviation,
        Boolean isCanonical,
        Double canonicalFactor,
        String formula,
        String description
) {
}
