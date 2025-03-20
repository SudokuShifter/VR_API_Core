package com.inlinegroup.vrcalculationbackend.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VRValidationDataResponse {
    private String zifObjectName;
    private String zifObjectUid;
    private Double wct;
    private Double gasCondensateFactor;
}
