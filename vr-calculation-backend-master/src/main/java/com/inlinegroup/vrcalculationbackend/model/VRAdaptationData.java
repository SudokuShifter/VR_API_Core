package com.inlinegroup.vrcalculationbackend.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Table(name = "vr_adaptation_data")
public class VRAdaptationData {
    @Id
    private Long id;

    @Column("vr_zif_objects_id")
    private Long vrZifObjectsId;

    private String name;

    @Column("choke_percent_adapt")
    private List<Double> chokeAdaptPercent;

    @Column("choke_value_adapt")
    private List<Double> chokeAdaptValue;

    @Column("date_start")
    private LocalDateTime dateStart;

    @Column("date_end")
    private LocalDateTime dateEnd;

    @Column("creation_date")
    private LocalDateTime creationDate;
}
