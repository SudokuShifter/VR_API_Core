package com.inlinegroup.vrcalculationbackend.exceptions;

import com.inlinegroup.vrcalculationbackend.api.fmm.HttpValidationErrorDto;
import com.inlinegroup.vrcalculationbackend.api.zif.common.ProblemDetailsDto;
import lombok.Getter;

@Getter
public class VRNotFoundClientException extends Exception {
    private final transient ProblemDetailsDto problemDetailsDto;
    private final transient HttpValidationErrorDto httpValidationErrorDto;

    public VRNotFoundClientException(String message) {
        super(message);
        this.problemDetailsDto = ProblemDetailsDto.builder().build();
        this.httpValidationErrorDto = HttpValidationErrorDto.builder().build();
    }

    public VRNotFoundClientException(String message, Object errorEntity) {
        super(message);
        if (errorEntity instanceof ProblemDetailsDto dto) {
            this.problemDetailsDto = dto;
            this.httpValidationErrorDto = HttpValidationErrorDto.builder().build();
        } else if (errorEntity instanceof HttpValidationErrorDto dto) {
            this.httpValidationErrorDto = dto;
            this.problemDetailsDto = ProblemDetailsDto.builder().build();
        } else {
            this.problemDetailsDto = ProblemDetailsDto.builder().build();
            this.httpValidationErrorDto = HttpValidationErrorDto.builder().build();
        }
    }
}