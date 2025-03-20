package com.inlinegroup.vrcalculationbackend.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class TagDataPointTimeDto {
    private String id;
    private String name;
    private Double value;
    private Double valueWithoutCF;
    private String timestamp;
}
