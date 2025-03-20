package com.inlinegroup.vrcalculationbackend.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class VRAdaptationDataResponse {
    private String zifObjectName;
    private String zifObjectUid;
    private String adaptName;
    private List<Double> dChokePercentAdapt;
    private List<Double> cChokeAdapt;
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private LocalDateTime creationDate;
}
