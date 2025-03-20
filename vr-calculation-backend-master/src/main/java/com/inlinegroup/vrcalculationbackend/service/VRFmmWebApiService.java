package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.fmm.*;
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
public class VRFmmWebApiService {

    private final VRCalcConfig config;
    private final WebClientExecutionService webClientExecutionService;

    public VRFmmWebApiService(VRCalcConfig config, WebClientExecutionService webClientExecutionService) {
        this.config = config;
        this.webClientExecutionService = webClientExecutionService;
    }

    /**
     * Метод выполнения расчета расхода на штуцере. Тип FMM
     * В случае получения статусов 422 будет выброшено исключение VRValidationException.
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     *
     * @param fmmTaskDto        параметры расчета
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return FMMTaskSolutionDto
     */
    public Mono<TaskSolutionDto<FMMSolutionDto>> execFmmTaskNS(Mono<FMMTaskDto> fmmTaskDto, String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config.getFmmScheme(), config.getFmmHost(), config.getFmmPort())
                .setPath(config.getFmmUriCalcFmmTask())
                .build();
        Mono<TaskSolutionDto<FMMSolutionDto>> result = webClientExecutionService.
                executeMonoPostRequest(uri.getUri(), fmmTaskDto, AppType.VR, new ParameterizedTypeReference<>() {
                }, "Execute FMM Task. " + logAdditionalData);
        return result.flatMap(solution -> validateVRCalcResponse(solution, logAdditionalData));
    }

    /**
     * Метод выполнения расчета коэффициента адаптации.
     * В случае получения статусов 422 будет выброшено исключение VRValidationException.
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     *
     * @param adaptTaskDto      параметры расчета
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return MLTaskSolutionDto
     */
    public Mono<TaskSolutionDto<AdaptSolutionDto>> execAdaptTaskNS(Mono<AdaptTaskDto> adaptTaskDto,
                                                                   String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config.getFmmScheme(), config.getFmmHost(), config.getFmmPort())
                .setPath(config.getFmmUriCalcAdaptTask())
                .build();
        Mono<TaskSolutionDto<AdaptSolutionDto>> result = webClientExecutionService.
                executeMonoPostRequest(uri.getUri(), adaptTaskDto, AppType.VR, new ParameterizedTypeReference<>() {
                }, "Execute ADAPTATION Task. " + logAdditionalData);
        return result.flatMap(solution -> validateVRCalcResponse(solution, logAdditionalData));
    }


    /**
     * Метод выполнения задачи валидации.
     * В случае получения статусов 422 будет выброшено исключение VRValidationException.
     * В случае получения статусов 5xx будет выброшено исключение VRClientException.
     *
     * @param validateTaskDto   параметры расчета
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return MLTaskSolutionDto
     */
    public Mono<TaskSolutionDto<ValidationSolutionDto>> execValidateTaskNS(Mono<ValidateTaskDto> validateTaskDto,
                                                                           String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config.getFmmScheme(), config.getFmmHost(), config.getFmmPort())
                .setPath(config.getFmmUriCalcValidateTask())
                .build();
        Mono<TaskSolutionDto<ValidationSolutionDto>> result = webClientExecutionService.
                executeMonoPostRequest(uri.getUri(), validateTaskDto, AppType.VR, new ParameterizedTypeReference<>() {
                }, "Execute VALIDATION Task. " + logAdditionalData);
        return result.flatMap(solution -> validateVRCalcResponse(solution, logAdditionalData));
    }

    private <T> Mono<TaskSolutionDto<T>> validateVRCalcResponse(TaskSolutionDto<T> data, String logAdditionalData) {
        if (!data.isSuccess()) {
            String jsonError = getTextRequestBodyMsg(data.getErrors());
            log.error("External FMM calculation module error. " + logAdditionalData + " Msg: " + jsonError);
            return Mono.error(new VRApiException("External FMM calculation module error", data.getErrors()));
        } else {
            if(data.getSolution() instanceof ValidationSolutionDto validationSolutionDto &&
                    (validationSolutionDto.wct() == null || validationSolutionDto.gasCondensateFactor() == null)){
                    log.error("The validation task returned null values. " + logAdditionalData + " WCT = " +
                            validationSolutionDto.wct() + " gasCondensateFactor = " +
                            validationSolutionDto.gasCondensateFactor());
                    return Mono.error(new VRApiException("The validation task returned null values."));
            }
        }
        return Mono.just(data);
    }
}