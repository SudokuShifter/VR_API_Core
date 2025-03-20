package com.inlinegroup.vrcalculationbackend.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VRValidationRecordResponse {
    private String zifObjectUid;
    private Double wct;
    private Double gasCondensateFactor;
    private Boolean isUserValue;
}
