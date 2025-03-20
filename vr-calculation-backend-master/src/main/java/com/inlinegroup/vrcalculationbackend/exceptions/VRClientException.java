package com.inlinegroup.vrcalculationbackend.exceptions;

import com.inlinegroup.vrcalculationbackend.api.fmm.HttpValidationErrorDto;
import com.inlinegroup.vrcalculationbackend.api.zif.common.ProblemDetailsDto;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class VRClientException extends Exception {
    private final transient ProblemDetailsDto problemDetailsDto;
    private final transient HttpValidationErrorDto httpValidationErrorDto;
    private final transient HttpStatusCode status;

    public VRClientException(String message, HttpStatusCode status) {
        super(message);
        this.status = status;
        this.problemDetailsDto = ProblemDetailsDto.builder().build();
        this.httpValidationErrorDto = HttpValidationErrorDto.builder().build();
    }

    public VRClientException(String message, Object errorEntity, HttpStatusCode status) {
        super(message);
        this.status = status;
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
