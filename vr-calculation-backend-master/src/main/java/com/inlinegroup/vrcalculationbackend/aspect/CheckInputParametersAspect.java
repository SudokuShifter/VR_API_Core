package com.inlinegroup.vrcalculationbackend.aspect;

import com.inlinegroup.vrcalculationbackend.exceptions.VRParameterApiException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.TIME_FORMAT_UTC_ISO8601;
import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.ZIF_UID_LENGTH;
import static com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils.correctEndDate;

@Aspect
@Component
@Slf4j
public class CheckInputParametersAspect {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_FORMAT_UTC_ISO8601);
    private static final String EX_MSG_PARAM_IS_NULL = "Parameter value is null";
    private static final String EX_MSG_WRONG_PARAM = "Invalid parameter value";
    private static final String EX_MSG_BAD_ID = "Invalid UID value";
    private static final String EX_MSG_BAD_DATE_FORMAT = "Invalid date value or format. " +
            "Format date 2023-10-01T10:10:00.000Z. The date must be a multiple of 30 seconds";
    private static final String BAD_MSG_REQUEST = "Bad request. ";

    @Pointcut(value = "@annotation(ValidateParams)")
    public void checkParamMethod() {
    }

    @SuppressWarnings("unchecked")
    @Around("checkParamMethod()")
    public <T> Publisher<T> execAdviceCheckParamMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        CodeSignature methodSignature = (CodeSignature) joinPoint.getSignature();
        Class<T> returnType = ((MethodSignature) methodSignature).getReturnType();
        String[] sigParamNames = methodSignature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();
        for (int i = 0; i < paramValues.length; ++i) {
            if (paramValues[i] == null) {
                log.warn(BAD_MSG_REQUEST + EX_MSG_PARAM_IS_NULL);
                return throwError(returnType, EX_MSG_PARAM_IS_NULL);
            }
            if (paramValues[i] instanceof String str && str.isEmpty()) {
                log.warn(BAD_MSG_REQUEST + EX_MSG_WRONG_PARAM);
                return throwError(returnType, EX_MSG_WRONG_PARAM);
            }
            if (paramValues[i] instanceof String str && !checkUID(sigParamNames[i], str)) {
                log.warn(BAD_MSG_REQUEST + EX_MSG_BAD_ID);
                return throwError(returnType, EX_MSG_BAD_ID);
            }
            if (paramValues[i] instanceof String str && !checkTime(sigParamNames[i], str)) {
                log.warn(BAD_MSG_REQUEST + EX_MSG_BAD_DATE_FORMAT);
                return throwError(returnType, EX_MSG_BAD_DATE_FORMAT);
            }
            if (paramValues[i] instanceof String str) {
                paramValues[i] = checkEndTime(sigParamNames[i], str);
            }
        }
        return (Publisher<T>) joinPoint.proceed(joinPoint.getArgs());
    }

    public static <T> Publisher<T> throwError(Class<T> t, String msg) {
        if (Mono.class == t) {
            return Mono.error(new VRParameterApiException(msg));
        }
        return Flux.error(new VRParameterApiException(msg));
    }

    private boolean checkUID(String paramName, String value) {
        if (!paramName.toLowerCase().contains("id")) {
            return true;
        }
        return value.length() == ZIF_UID_LENGTH;
    }

    private boolean checkTime(String paramName, String value) {
        if (!paramName.toLowerCase().contains("time") && !paramName.toLowerCase().contains("date")) {
            return true;
        }
        try {
            LocalDateTime time = LocalDateTime.parse(value, formatter);
            int second = time.getSecond();
            int nanoSecond = time.getNano();
            if ((second == 0 || second == 30) && nanoSecond == 0) {
                return true;
            }
        } catch (DateTimeParseException ex) {
            return false;
        }
        return false;
    }

    /**
     * Метод корректировки передаваемой даты. Если дата окончания интервала кратна месяцу, то производится
     * корректировка даты - вычитание из нее шага платформы
     */
    private String checkEndTime(String paramName, String value) {
        if (!(paramName.toLowerCase().contains("time")) && !(paramName.toLowerCase().contains("date"))) {
            return value;
        }
        if (!(paramName.toLowerCase().contains("end")) && !(paramName.toLowerCase().contains("right"))) {
            return value;
        }
        try {
            String curEndDate = correctEndDate(value);
            log.debug("Correction of the end date in the request. " + value + " to " + curEndDate);
            return curEndDate;
        } catch (DateTimeParseException ex) {
            return value;
        }
    }
}