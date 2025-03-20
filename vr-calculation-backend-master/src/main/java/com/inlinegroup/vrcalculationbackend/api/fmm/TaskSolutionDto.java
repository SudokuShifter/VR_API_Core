package com.inlinegroup.vrcalculationbackend.api.fmm;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class TaskSolutionDto<T> {
    private boolean success;
    private Object errors;
    private Object message;
    private T solution;
}
