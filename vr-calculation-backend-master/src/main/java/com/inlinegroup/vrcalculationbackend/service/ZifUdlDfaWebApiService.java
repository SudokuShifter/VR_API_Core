package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.zif.udl.RtvInfoDto;
import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import com.inlinegroup.vrcalculationbackend.exceptions.VRNotFoundClientException;
import com.inlinegroup.vrcalculationbackend.service.enums.AppType;
import com.inlinegroup.vrcalculationbackend.service.utils.ZifUri;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.*;

@Component
@Slf4j
public class ZifUdlDfaWebApiService {

    private static final String PROPERTY_UID_NAME = " Property UID: ";
    private final VRCalcConfig config;
    private final WebClientExecutionService webClientExecutionService;

    public ZifUdlDfaWebApiService(VRCalcConfig config, WebClientExecutionService webClientExecutionService) {
        this.config = config;
        this.webClientExecutionService = webClientExecutionService;
    }

    /**
     * Метод получения архивных значений тега за период времени.
     * В случае отсутствия данных за метку времени возвращается пустой массив
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     * В случае отсутствия ответа будет выброшено исключение WebClientRequestException.
     *
     * @param propertyId        id модели
     * @param timeLeft          левая граница времени UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param timeRight         правая граница времени UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return RtvInfoDto[]
     */
    public Mono<RtvInfoDto[]> getRtdRawValues(String propertyId, String timeLeft, String timeRight,
                                              String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config)
                .setPath(config.getUriUdlDfaWebApiGetValues())
                .addPathId(propertyId)
                .addQueryParams(ZIF_UDL_QP_TIME_LEFT, timeLeft)
                .addQueryParams(ZIF_UDL_QP_TIME_RIGHT, timeRight)
                .build();
        Mono<RtvInfoDto[]> result = webClientExecutionService
                .executeMonoGetRequest(uri.getUri(), AppType.ZIF, new ParameterizedTypeReference<>() {
                }, "Get tag values over a period of time. " + logAdditionalData +
                        getObjectLogString(propertyId, timeLeft, timeRight, ""));
        return result.flatMap(rtvInfoDtoArr ->
                checkRtvInfoDto(rtvInfoDtoArr, "One or more tag values is missing. " + logAdditionalData +
                        getObjectLogString(propertyId, timeLeft, timeRight, "")));
    }

    /**
     * Метод получения архивного значение тега за определенную дату.
     * В случае отсутствия данных за метку времени возвращается пустой массив
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     * В случае отсутствия ответа будет выброшено исключение WebClientRequestException.
     *
     * @param propertyId        id модели
     * @param time              метка времени UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return RtvInfoDto[]
     */
    public Mono<RtvInfoDto[]> getRtdRawValue(String propertyId, String time,
                                             String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config)
                .setPath(config.getUriUdlDfaWebApiGetValue())
                .addPathId(propertyId)
                .addQueryParams(ZIF_UDL_QP_TIME, time)
                .build();
        Mono<RtvInfoDto[]> result = webClientExecutionService
                .executeMonoGetRequest(uri.getUri(), AppType.ZIF, new ParameterizedTypeReference<>() {
                }, "Get tag value per unit time. " + logAdditionalData +
                        getObjectLogString(propertyId, time, "", ""));
        return result.flatMap(rtvInfoDtoArr ->
                checkRtvInfoDto(rtvInfoDtoArr, "Tag values is missing. " + logAdditionalData +
                        getObjectLogString(propertyId, time, "", "")));
    }

    /**
     * Метод получения следующего за датой архивного значение тега.
     * В случае отсутствия данных за метку времени возвращается пустой массив
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     * В случае отсутствия ответа будет выброшено исключение WebClientRequestException.
     *
     * @param propertyId        id модели
     * @param time              метка времени UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return RtvInfoDto[]
     */
    public Mono<RtvInfoDto[]> getRtdRawNextValue(String propertyId, String time,
                                                 boolean onlyGoodValues, String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config)
                .setPath(config.getUriUdlDfaWebApiGetNext())
                .addPathId(propertyId)
                .addQueryParams(ZIF_UDL_QP_TIME, time)
                .addQueryParams(ZIF_UDL_QP_ONLY_GOOD_VALUE, String.valueOf(onlyGoodValues))
                .build();
        Mono<RtvInfoDto[]> result = webClientExecutionService
                .executeMonoGetRequest(uri.getUri(), AppType.ZIF, new ParameterizedTypeReference<>() {
                }, "Get NEXT tag value per unit time. " + logAdditionalData +
                        getObjectLogString(propertyId, time, "", ""));
        return result.flatMap(rtvInfoDtoArr ->
                checkRtvInfoDto(rtvInfoDtoArr, "Tag values is missing. " + logAdditionalData +
                        getObjectLogString(propertyId, time, "", "")));
    }

    /**
     * Метод записи значения в тэг за определенную дату.
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     * В случае отсутствия ответа будет выброшено исключение WebClientRequestException.
     *
     * @param propertyId        id модели
     * @param time              метка времени UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param value             значение тега
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return RtvInfoDto[]
     */
    public Mono<RtvInfoDto[]> writeRtdRawValue(String propertyId, String time, String value, String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config)
                .setPath(config.getUriUdlDfaWebApiWriteValue())
                .addPathId(propertyId)
                .addQueryParams(ZIF_UDL_QP_TIME, time)
                .addQueryParams(ZIF_UDL_QP_VALUE, value)
                .build();
        return webClientExecutionService
                .executeMonoPostRequest(uri.getUri(), AppType.ZIF, new ParameterizedTypeReference<>() {
                }, "Write tag value. " + logAdditionalData +
                        getObjectLogString(propertyId, time, "", value));
    }

    private Mono<RtvInfoDto[]> checkRtvInfoDto(RtvInfoDto[] rtvInfoDtoArr, String logMsg) {
        if (rtvInfoDtoArr == null || rtvInfoDtoArr.length == 0) {
            return Mono.error(new VRNotFoundClientException(logMsg));
        }
        return Mono.just(rtvInfoDtoArr);
    }

    private static String getObjectLogString(String propertyId, String timeLeft, String timeRight, String value) {
        String time = timeRight.isEmpty() ? " Time: " + timeLeft : " Time from:  " + timeLeft + " to:  " + timeRight;
        String val = value.isEmpty() ? "" : " Value: " + value;
        return PROPERTY_UID_NAME + propertyId + time + val;
    }
}
