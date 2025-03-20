package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.TagDataPointTimeDto;
import com.inlinegroup.vrcalculationbackend.api.fmm.FMMTaskDto;
import com.inlinegroup.vrcalculationbackend.api.ml.FeaturesDto;
import com.inlinegroup.vrcalculationbackend.api.ml.PredictRequestDto;
import com.inlinegroup.vrcalculationbackend.api.ml.PredictResponseDto;
import com.inlinegroup.vrcalculationbackend.api.zif.om.OmPropertyDto;
import com.inlinegroup.vrcalculationbackend.api.zif.udl.RtvInfoDto;
import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import com.inlinegroup.vrcalculationbackend.exceptions.VRNotFoundApiException;
import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;
import com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.*;
import static com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils.getTimeMinusDays;

/**
 * Передаваемые и возвращаемые данные в модуле расчетов ML не переведены в систему СИ
 * !!! Сервис производит пересчет входных параметров в градусы цельсия на уровне сопоставления DTO, т.к. все
 * функции запроса данных из платформы Цифра выполняют перевод в систему СИ в соответствии с коэффициентами пересчета
 * (canonical factor), которые так же хранятся в платформе Цифра
 * !!! Отключен пересчет результата расчета ML модуля.
 */
@Service
@Slf4j
public class MLTaskService {

    public static final String EX_MSG_TAG_DATA_IS_MISSING = "One or more tag values are missing";
    private final VRStorageService vrStorageService;
    private final ZifObjectModelService zifObjectModelService;
    private final VRAdaptationAndValidationService vrAdaptationAndValidationService;
    private final VRMlWebApiService vrMlWebApiService;
    private final VRCalcConfig config;

    public MLTaskService(VRStorageService vrStorageService,
                         ZifObjectModelService zifObjectModelService,
                         VRAdaptationAndValidationService vrAdaptationAndValidationService,
                         VRMlWebApiService vrMlWebApiService,
                         VRCalcConfig config) {
        this.vrStorageService = vrStorageService;
        this.zifObjectModelService = zifObjectModelService;
        this.vrAdaptationAndValidationService = vrAdaptationAndValidationService;
        this.vrMlWebApiService = vrMlWebApiService;
        this.config = config;
    }

    /**
     * Метод выполнения ML задачи за указанную точку времени.
     * Температура подается в градусах Цельсия
     * Результирующее значение сохраняется БЕЗ УЧЕТА КОЭФФИЦИЕНТА ПЕРЕСЧЕТА в систему СИ, т.к. модуль расчетов
     * возвращает значение в ksm3/h
     *
     * @param objectId      uid объекта модели (скважина)
     * @param date          метка времени UTC in ISO8601 format (2023-11-20T13:17:00.000Z)
     * @param logObjectName дополнительные сведения для операции логирования
     */
    public Flux<Void> executeMLTask(String objectId, String date, String logObjectName) {
        return vrMlWebApiService.execAndCheckMLTaskNS(getRequestDataMLTask(objectId, date), logObjectName)
                .flatMapMany(response ->
                        saveMLSolutionToPlatformWithoutCanonicalFactor(response, objectId, date, logObjectName))
                .flatMap(res -> Flux.empty());
    }

    /**
     * Метод выполнения ML задачи за указанную точку времени. Использует данные FMM задачи.
     * Температура подается в градусах Цельсия. Из объекта fmmTaskDto переводятся в градусы Цельсия только
     * используемые значения (P_BUF, T_OUT)
     * Результирующее значение сохраняется БЕЗ УЧЕТА КОЭФФИЦИЕНТА ПЕРЕСЧЕТА в систему СИ, т.к. модуль расчетов
     * возвращает значение в ksm3/h
     *
     * @param objectId      uid объекта модели (скважина)
     * @param date          метка времени UTC in ISO8601 format (2023-11-20T13:17:00.000Z)
     * @param fmmTaskDto    DTO из FMM задачи
     * @param logObjectName дополнительные сведения для операции логирования
     */
    public Flux<Void> executeMLTask(String objectId, String date, String logObjectName, Mono<FMMTaskDto> fmmTaskDto) {
        return vrMlWebApiService.execAndCheckMLTaskNS(getRequestDataMLTask(objectId, date, fmmTaskDto), logObjectName)
                .flatMapMany(response ->
                        saveMLSolutionToPlatformWithoutCanonicalFactor(response, objectId, date, logObjectName))
                .flatMap(res -> Flux.empty());
    }

    /**
     * Метод выполнения ML задачи за промежуток времени. Задачи выполняются последовательно
     *
     * @param objectId          uid объекта модели (скважина)
     * @param timeLeft          левая граница времени UTC in ISO8601 format (2023-11-20T13:17:00.000Z)
     * @param timeRight         правая граница времени  UTC in ISO8601 format (2023-11-20T13:17:00.000Z)
     * @param logAdditionalData дополнительные сведения для операции логирования
     */
    public Mono<Void> executeMLTaskDuration(String objectId, String timeLeft, String timeRight,
                                            String logAdditionalData) {
        return Flux.fromIterable(TimeUtils.getTimeList(timeLeft, timeRight))
                .publishOn(Schedulers.boundedElastic())
                .concatMap(date -> Mono.just(saveExecuteMLTaskWithBlock(
                        objectId,
                        date,
                        logAdditionalData)))
                .then();
    }

    private String saveExecuteMLTaskWithBlock(String objectId, String date, String logAdditionalData) {
        try {
            vrAdaptationAndValidationService
                    .executeTaskValidation(
                            objectId,
                            getTimeMinusDays(date, config.getVrTaskValidationCountDays()),
                            date)
                    .thenMany(executeMLTask(objectId, date, logAdditionalData))
                    .blockFirst();
        } catch (Exception ex) {
            log.error("ML task error for time: " + date + " Error msg: " + ex.getMessage());
        }
        return date;
    }

    /**
     * Метод сохранения значения ML модели БЕЗ УЧЕТА КОЭФФИЦИЕНТА ПЕРЕСЧЕТА
     */
    protected Flux<RtvInfoDto[]> saveMLSolutionToPlatformWithoutCanonicalFactor(
            PredictResponseDto predictResponse, String parentObjectId, String date, String logObjectName) {
        return getMLObjectProperty(parentObjectId, logObjectName)
                .filter(omPropertyDto -> getMLTaskResponseTagsNameList().contains(omPropertyDto.name().toLowerCase()))
                .flatMap(omPropertyDto -> vrAdaptationAndValidationService
                        .saveTagToPlatform(omPropertyDto,
                                date,
                                getValueByPropertyAndMLResponse(omPropertyDto, predictResponse),
                                "ML MODEL - " + logObjectName,
                                false));
    }

    private static Double getValueByPropertyAndMLResponse(OmPropertyDto omPropertyDto,
                                                          PredictResponseDto predictResponse) {
        if (omPropertyDto.name().equalsIgnoreCase(ZIF_ML_TAG_NAME_GAS_CONSUMPTION)) {
            return predictResponse.getGasConsumption();
        }
        if (omPropertyDto.name().equalsIgnoreCase(ZIF_ML_TAG_NAME_GAS_ERROR)) {
            return predictResponse.getGasError();
        }
        return null;
    }

    protected Flux<OmPropertyDto> getMLObjectProperty(String parentObjectId, String logObjectName) {
        return vrAdaptationAndValidationService
                .getObjectByObjectParentId(parentObjectId, ZIF_ML_OBJECT_NAME, logObjectName)
                .flatMapMany(omObjectDto ->
                        zifObjectModelService.getObjectPropertiesByObjectIdInFlux(omObjectDto.id(), logObjectName));
    }

    protected Mono<PredictRequestDto> getRequestDataMLTask(String objectId, String date) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        Mono<List<TagDataPointTimeDto>> tagAdditionalData = vrStorageService
                .getAdditionalObjectByNameAndMainObject(objectId, ZIF_MANIFOLD_OBJECT_NAME.toLowerCase())
                .zipWith(vrZifMainObject)
                .flatMap(tuple -> vrAdaptationAndValidationService.getTagDataByObjectId(
                        tuple.getT1().getZifUid(),
                        date,
                        tuple.getT2().getName(),
                        vrAdaptationAndValidationService.getVRAdditionalTagsNameList(),
                        false));
        Mono<List<TagDataPointTimeDto>> tagData = vrZifMainObject
                .flatMap(mainObject -> vrAdaptationAndValidationService.getTagDataByObjectId(
                        objectId,
                        date,
                        mainObject.getName(),
                        getMLTaskRequestTagsNameList(),
                        false));
        return Mono.just(new PredictRequestDto())
                .zipWith(vrZifMainObject)
                .flatMap(tuple -> mapperTagDataToMLTaskRequest(
                        tuple.getT1(), tuple.getT2(), ML_TARGET_NAME_GAS_CONSUMPTION))
                .zipWith(tagAdditionalData)
                .flatMap(tuple -> mapperTagDataToMLTaskRequest(tuple.getT1(), tuple.getT2()))
                .zipWith(tagData)
                .flatMap(tuple -> mapperTagDataToMLTaskRequest(tuple.getT1(), tuple.getT2()));
    }

    protected Mono<PredictRequestDto> getRequestDataMLTask(String objectId, String date, Mono<FMMTaskDto> fmmTaskDto) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        Mono<List<TagDataPointTimeDto>> tagData = vrZifMainObject
                .flatMap(mainObject -> vrAdaptationAndValidationService.getTagDataByObjectId(
                        objectId,
                        date,
                        mainObject.getName(),
                        getMLTaskRequestShortTagsNameList(), false));
        return Mono.just(new PredictRequestDto())
                .zipWith(vrZifMainObject)
                .flatMap(tuple -> mapperTagDataToMLTaskRequest(
                        tuple.getT1(), tuple.getT2(), ML_TARGET_NAME_GAS_CONSUMPTION))
                .zipWith(fmmTaskDto)
                .map(tuple -> {
                    PredictRequestDto predictRequestDto = tuple.getT1();
                    FMMTaskDto fmmTask = tuple.getT2();
                    predictRequestDto.setFeatures(fmmTask.getDataForMLWithoutCF());
                    return predictRequestDto;
                })
                .zipWith(tagData)
                .flatMap(tuple -> mapperTagDataToMLTaskRequest(tuple.getT1(), tuple.getT2()));
    }

    private Mono<PredictRequestDto> mapperTagDataToMLTaskRequest(PredictRequestDto predictRequest,
                                                                 List<TagDataPointTimeDto> tagDataPointTimeList) {
        if (tagDataPointTimeList.isEmpty()) {
            return Mono.error(new VRNotFoundApiException(EX_MSG_TAG_DATA_IS_MISSING));
        }
        if (predictRequest.getFeatures() == null) {
            predictRequest.setFeatures(FeaturesDto.builder().build());
        }
        for (TagDataPointTimeDto tag : tagDataPointTimeList) {
            switch (tag.getName().toLowerCase()) {
                case ZIF_MANIFOLD_TAG_NAME_P_OUT:
                    predictRequest.getFeatures().setPOut(tag.getValue());
                    break;
                case ZIF_MANIFOLD_TAG_NAME_T_OUT:
                    predictRequest.getFeatures().setTOut(tag.getValue());
                    break;
                case ZIF_TAG_NAME_D_CHOKE_PERCENT_TIMED:
                    predictRequest.getFeatures().setDChokePercent(tag.getValue());
                    break;
                case ZIF_TAG_NAME_T_BUF:
                    predictRequest.getFeatures().setTBuf(tag.getValue());
                    break;
                case ZIF_TAG_NAME_P_BUF:
                    predictRequest.getFeatures().setPBuf(tag.getValue());
                    break;
                case ZIF_TAG_NAME_P_DOWN_HOLE:
                    predictRequest.getFeatures().setPDownHole(tag.getValue());
                    break;
                case ZIF_TAG_NAME_T_DOWN_HOLE:
                    predictRequest.getFeatures().setTDownHole(tag.getValue());
                    break;
                case ZIF_TAG_NAME_Q_GAS:
                    predictRequest.getFeatures().setQGas(tag.getValue());
                    break;
                default:
            }
        }
        return Mono.just(predictRequest);
    }

    private Mono<PredictRequestDto> mapperTagDataToMLTaskRequest(PredictRequestDto predictRequest,
                                                                 VRZifMainObject vrZifMainObject,
                                                                 String targetName) {
        predictRequest.setWellId(vrZifMainObject.getHoleProjectId());
        predictRequest.setTargetName(targetName);
        return Mono.just(predictRequest);
    }

    private List<String> getMLTaskRequestTagsNameList() {
        return new ArrayList<>(Arrays.asList(
                ZIF_TAG_NAME_D_CHOKE_PERCENT_TIMED.toLowerCase(),
                ZIF_TAG_NAME_T_BUF.toLowerCase(),
                ZIF_TAG_NAME_P_BUF.toLowerCase(),
                ZIF_TAG_NAME_P_DOWN_HOLE.toLowerCase(),
                ZIF_TAG_NAME_T_DOWN_HOLE.toLowerCase(),
                ZIF_TAG_NAME_Q_GAS.toLowerCase()
        ));
    }

    private List<String> getMLTaskRequestShortTagsNameList() {
        return new ArrayList<>(Arrays.asList(
                ZIF_TAG_NAME_P_DOWN_HOLE.toLowerCase(),
                ZIF_TAG_NAME_T_DOWN_HOLE.toLowerCase()
        ));
    }

    private List<String> getMLTaskResponseTagsNameList() {
        return new ArrayList<>(List.of(
                ZIF_ML_TAG_NAME_GAS_CONSUMPTION.toLowerCase(),
                ZIF_ML_TAG_NAME_GAS_ERROR.toLowerCase()
        ));
    }
}