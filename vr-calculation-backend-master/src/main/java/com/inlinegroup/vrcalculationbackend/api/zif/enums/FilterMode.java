package com.inlinegroup.vrcalculationbackend.api.zif.enums;

import lombok.Getter;

@Getter
public enum FilterMode {
    ONLY_ELEMENT("ONLYELEMENT"), WITH_PARENT("WITHPARENT");

    private final String mode;

    FilterMode(String mode) {
        this.mode = mode;
    }

}