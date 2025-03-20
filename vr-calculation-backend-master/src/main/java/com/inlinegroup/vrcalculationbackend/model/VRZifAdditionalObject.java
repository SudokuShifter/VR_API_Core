package com.inlinegroup.vrcalculationbackend.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@Table(name = "vr_zif_additional_objects")
public class VRZifAdditionalObject {
    @Id
    private Long id;

    private String name;

    @Column("zif_uid")
    private String zifUid;

    @Column("creation_date")
    private LocalDateTime creationDate;
}
