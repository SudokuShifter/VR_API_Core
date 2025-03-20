package com.inlinegroup.vrcalculationbackend.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@Table(name = "vr_zif_objects")
public class VRZifMainObject {
    @Id
    private Long id;

    private String name;

    @Column("hole_project_id")
    private Integer holeProjectId;

    @Column("zif_uid")
    private String zifUid;

    @Column("active_adaptation_value_id")
    private Long activeAdaptationValueId;

    @Column("creation_date")
    private LocalDateTime creationDate;

    @Column("active_vr_type")
    private String activeVrType;

    @Column("current_date_scheduler")
    private LocalDateTime currentDateScheduler;

    @Column("scheduler_status")
    private String schedulerStatus;
}
