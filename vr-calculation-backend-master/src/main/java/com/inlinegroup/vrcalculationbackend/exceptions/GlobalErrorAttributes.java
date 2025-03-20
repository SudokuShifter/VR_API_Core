package com.inlinegroup.vrcalculationbackend.exceptions;

import com.inlinegroup.vrcalculationbackend.api.fmm.DetailErrorDto;
import com.inlinegroup.vrcalculationbackend.api.fmm.HttpValidationErrorDto;
import com.inlinegroup.vrcalculationbackend.api.zif.common.ProblemDetailsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.COMMON_RESPONSE_ON_500_ERROR;

@Component
@Slf4j
public class GlobalErrorAttributes extends DefaultErrorAttributes {
    public static final String RESPONSE_PARAM_STATUS = "status";
    public static final String RESPONSE_PARAM_ERROR = "error";
    public static final String RESPONSE_PARAM_REQUEST_ID = "requestId";
    public static final String RESPONSE_PARAM_MSG = "message";
    public static final String LOG_MSG = " , massage: ";

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> map = super.getErrorAttributes(request, options);
        map.remove(RESPONSE_PARAM_ERROR);
        map.remove(RESPONSE_PARAM_REQUEST_ID);
        Throwable ex = getError(request);
        if (ex instanceof WebClientRequestException || ex instanceof WebClientResponseException) {
            map.put(RESPONSE_PARAM_STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
            map.put(RESPONSE_PARAM_MSG, ex.getMessage());
            log.error(ex.getClass() + LOG_MSG + ex.getMessage());
            return map;
        }
        if (ex instanceof VRApiException exVra) {
            map.put(RESPONSE_PARAM_STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
            map.put(RESPONSE_PARAM_MSG, ex.getMessage());
            addUnknownError(map, exVra);
            log.error(ex.getClass() + LOG_MSG + ex.getMessage());
            return map;
        }
        if (ex instanceof VRClientException exVrc) {
            map.put(RESPONSE_PARAM_STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
            map.put(RESPONSE_PARAM_MSG, ex.getMessage());
            addErrorFromProblemDetails(map, exVrc);
            addErrorFromValidationError(map, exVrc);
            log.error(ex.getClass() + LOG_MSG + ex.getMessage());
            return map;
        }
        if (ex instanceof VRNotFoundClientException) {
            map.put(RESPONSE_PARAM_STATUS, HttpStatus.NOT_FOUND);
            map.put(RESPONSE_PARAM_MSG, ex.getMessage());
            log.warn(ex.getClass() + LOG_MSG + ex.getMessage());
            return map;
        }
        if (ex instanceof VRParameterApiException) {
            map.put(RESPONSE_PARAM_STATUS, HttpStatus.BAD_REQUEST);
            map.put(RESPONSE_PARAM_MSG, ex.getMessage());
            log.warn(ex.getClass() + LOG_MSG + ex.getMessage());
            return map;
        }
        if (ex instanceof VRNotFoundApiException) {
            map.put(RESPONSE_PARAM_STATUS, HttpStatus.NOT_FOUND);
            map.put(RESPONSE_PARAM_MSG, ex.getMessage());
            log.warn(ex.getClass() + LOG_MSG + ex.getMessage());
            return map;
        }

        map.put(RESPONSE_PARAM_STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
        map.put(RESPONSE_PARAM_MSG, COMMON_RESPONSE_ON_500_ERROR);
        log.error(ex.getClass() + LOG_MSG + ex.getMessage());
        return map;
    }

    private void addErrorFromProblemDetails(Map<String, Object> map, VRClientException ex) {
        ProblemDetailsDto problemDetailsDto = ex.getProblemDetailsDto();
        if (problemDetailsDto == null) {
            return;
        }
        if (problemDetailsDto.detail() != null && !problemDetailsDto.detail().isEmpty()) {
            map.put("Detail", problemDetailsDto.detail());
        }
        if (problemDetailsDto.title() != null && !problemDetailsDto.title().isEmpty()) {
            map.put("Title", problemDetailsDto.title());
        }
    }

    private void addErrorFromValidationError(Map<String, Object> map, VRClientException ex) {
        HttpValidationErrorDto httpValidationErrorDto = ex.getHttpValidationErrorDto();
        if (httpValidationErrorDto == null || httpValidationErrorDto.detail() == null ||
                httpValidationErrorDto.detail().isEmpty()) {
            return;
        }
        for (DetailErrorDto detailErrorDto : httpValidationErrorDto.detail()) {
            if (!detailErrorDto.loc().isEmpty()) {
                int i = 0;
                for (Object obj : detailErrorDto.loc()) {
                    if (obj instanceof String || obj instanceof Integer) {
                        map.put("Loc " + i, obj.toString());
                        i++;
                    }
                }
            }
            map.put("Msg", detailErrorDto.msg());
            map.put("Type", detailErrorDto.type());
        }

    }

    private void addUnknownError(Map<String, Object> map, VRApiException ex) {
        if(ex.getUnknownError() != null){
            map.put("Msg", ex.getUnknownError());
        }
    }
}
