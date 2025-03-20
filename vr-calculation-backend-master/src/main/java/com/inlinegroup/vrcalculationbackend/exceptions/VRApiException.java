package com.inlinegroup.vrcalculationbackend.exceptions;

import lombok.Getter;

@Getter
public class VRApiException extends Exception {
    private final transient Object unknownError;

    public VRApiException(String message) {
        super(message);
        this.unknownError = null;
    }

    public VRApiException(String message, Object unknownError) {
        super(message);
        this.unknownError = unknownError;
    }
}
