package com.inlinegroup.vrcalculationbackend.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@Table(name = "vr_zif_object_2_additional_object")
public class VRZifObject2AdditionalObject {
    @Id
    private Long id;

    @Column("vr_zif_objects_id")
    private String vrZifObjectsId;

    @Column("vr_zif_additional_objects_id")
    private String vrZifAdditionalObjectsId;

    @Column("name_train")
    private String nameTrain;
}
