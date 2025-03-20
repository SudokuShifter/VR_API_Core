package com.inlinegroup.vrcalculationbackend.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@Table(name = "vr_validation_data")
public class VRValidationData {
    @Id
    private Long id;

    @Column("vr_zif_objects_id")
    private Long vrZifObjectsId;

    @Column("wct")
    private Double wct;

    @Column("gas_condensate_factor")
    private Double gasCondensateFactor;

    @Column("is_user_value")
    private Boolean isUserValue;

    @Column("date")
    private LocalDateTime date;
}
