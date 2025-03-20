package com.inlinegroup.vrcalculationbackend.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@Table(name = "vr_scheduler_status")
public class VRSchedulerStatus {
    @Id
    private String id;
    private String description;
}