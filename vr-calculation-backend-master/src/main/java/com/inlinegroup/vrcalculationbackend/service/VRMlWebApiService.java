package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.ml.AvailabilityModelRequestDto;
import com.inlinegroup.vrcalculationbackend.api.ml.AvailabilityModelResponseDto;
import com.inlinegroup.vrcalculationbackend.api.ml.PredictRequestDto;
import com.inlinegroup.vrcalculationbackend.api.ml.PredictResponseDto;
import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import com.inlinegroup.vrcalculationbackend.exceptions.VRApiException;
import com.inlinegroup.vrcalculationbackend.service.enums.AppType;
import com.inlinegroup.vrcalculationbackend.service.utils.ZifUri;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.inlinegroup.vrcalculationbackend.service.WebClientExecutionService.getTextRequestBodyMsg;

@Component
@Slf4j
public class VRMlWebApiService {
    private final VRCalcConfig config;
    private final WebClientExecutionService webClientExecutionService;

    public VRMlWebApiService(VRCalcConfig config, WebClientExecutionService webClientExecutionService) {
        this.config = config;
        this.webClientExecutionService = webClientExecutionService;
    }

    /**
     * Метод выполнения расчета целевого показателя. Тип ML
     * В случае получения не валидного результата будет выброшено исключение VRApiException.
     * В случае получения статусов 422 будет выброшено исключение VRValidationException.
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     *
     * @param mlRequestDto      параметры расчета
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return PredictResponseDto
     */
    public Mono<PredictResponseDto> execMLTaskNS(Mono<PredictRequestDto> mlRequestDto, String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config.getMlScheme(), config.getMlHost(), config.getMlPort())
                .setPath(config.getMlUriCalcMlTask())
                .build();
        Mono<PredictResponseDto> result = webClientExecutionService.
                executeMonoPostRequest(uri.getUri(), mlRequestDto, AppType.VR, new ParameterizedTypeReference<>() {
                }, "Execute ML Task. " + logAdditionalData);
        return result.flatMap(responseDto -> validateMLCalcResponse(responseDto, logAdditionalData));
    }

    /**
     * Метод проверки доступности модели и выполнения расчета целевого показателя. Тип ML
     * В случае получения не валидного результата будет выброшено исключение VRApiException.
     * В случае получения статусов 422 будет выброшено исключение VRValidationException.
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     *
     * @param mlRequestDto      параметры расчета
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return PredictResponseDto
     */
    public Mono<PredictResponseDto> execAndCheckMLTaskNS(Mono<PredictRequestDto> mlRequestDto,
                                                         String logAdditionalData) {
        return mlRequestDto.flatMap(mlRequest -> checkAvailabilityModel(
                        mlRequest.getWellId(),
                        mlRequest.getTargetName(),
                        logAdditionalData))
                .then(execMLTaskNS(mlRequestDto, logAdditionalData));
    }

    /**
     * Метод выполнения проверки доступности модели. Тип ML
     * В случае получения не валидного результата будет выброшено исключение VRApiException.
     * В случае получения статусов 422 будет выброшено исключение VRValidationException.
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     *
     * @param availability      объект со значением id скважины
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return AvailabilityModelResponseDto
     */
    public Mono<AvailabilityModelResponseDto> checkAvailabilityModel(Mono<AvailabilityModelRequestDto> availability,
                                                                     String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config.getMlScheme(), config.getMlHost(), config.getMlPort())
                .setPath(config.getMlUriAvailabilityOfModel())
                .build();
        Mono<AvailabilityModelResponseDto> result = webClientExecutionService.
                executeMonoPostRequest(uri.getUri(), availability, AppType.VR, new ParameterizedTypeReference<>() {
                }, "ML Task. Check availability model" + logAdditionalData);
        return result.flatMap(responseDto -> checkAvailabilityModel(responseDto, logAdditionalData));
    }

    public Mono<AvailabilityModelResponseDto> checkAvailabilityModel(Integer id, String targetName,
                                                                     String logAdditionalData) {
        AvailabilityModelRequestDto request = AvailabilityModelRequestDto.builder()
                .wellId(id)
                .targetName(targetName)
                .build();
        return checkAvailabilityModel(Mono.just(request), logAdditionalData);
    }

    /**
     * Метод проверки доступности модели.
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     *
     * @param logAdditionalData дополнительные сведения для операции логирования
     */
    public Mono<String> checkAvailabilityService(String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config.getMlScheme(), config.getMlHost(), config.getMlPort())
                .setPath(config.getMlUriAvailabilityOfService())
                .build();
        return webClientExecutionService.
                executeMonoGetRequest(uri.getUri(), AppType.VR, new ParameterizedTypeReference<>() {
                }, "ML Task. Check availability service . " + logAdditionalData);
    }

    private Mono<PredictResponseDto> validateMLCalcResponse(PredictResponseDto data, String logAdditionalData) {
        if (data.getError() != null) {
            String jsonError = getTextRequestBodyMsg(data.getError());
            log.error("External ML calculation module error. " + logAdditionalData + " Msg: " + jsonError);
            return Mono.error(new VRApiException("External ML calculation module error", data.getError()));
        } else {
            if (data.getGasConsumption() == null) {
                log.error("The ML task returned null values. " + logAdditionalData + " Расход по газу = null");
                return Mono.error(new VRApiException("The ML task returned null value."));
            }
        }
        return Mono.just(data);
    }

    private Mono<AvailabilityModelResponseDto> checkAvailabilityModel(AvailabilityModelResponseDto data,
                                                                      String logAdditionalData) {
        if (data.modelStatus() == null || !data.modelStatus()) {
            log.error("ML model is not available. " + logAdditionalData);
            return Mono.error(new VRApiException("ML model is not available"));
        }
        return Mono.just(data);
    }
}
