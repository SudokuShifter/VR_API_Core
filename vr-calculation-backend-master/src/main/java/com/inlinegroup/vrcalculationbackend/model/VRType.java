package com.inlinegroup.vrcalculationbackend.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@Table(name = "vr_type")
public class VRType {
    @Id
    private String id;
    private String description;
}