package com.inlinegroup.vrcalculationbackend.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TypeScheduler {
    FIXED_TIME("FIXED_TIME"), DATA_FOUND("DATA_FOUND"), SCHEDULER_OFF("SCHEDULER_OFF");

    public final String type;

    public static boolean contains(String type) {
        for (TypeScheduler typeScheduler : TypeScheduler.values()) {
            if (typeScheduler.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
