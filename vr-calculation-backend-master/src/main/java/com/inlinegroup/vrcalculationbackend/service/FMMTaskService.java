package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.ConstDataDto;
import com.inlinegroup.vrcalculationbackend.api.TagDataPointTimeDto;
import com.inlinegroup.vrcalculationbackend.api.fmm.FMMSolutionDto;
import com.inlinegroup.vrcalculationbackend.api.fmm.FMMTaskDto;
import com.inlinegroup.vrcalculationbackend.api.zif.om.OmPropertyDto;
import com.inlinegroup.vrcalculationbackend.api.zif.udl.RtvInfoDto;
import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import com.inlinegroup.vrcalculationbackend.exceptions.VRNotFoundApiException;
import com.inlinegroup.vrcalculationbackend.mapper.FMMMapper;
import com.inlinegroup.vrcalculationbackend.model.VRAdaptationData;
import com.inlinegroup.vrcalculationbackend.model.VRValidationData;
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

@Service
@Slf4j
public class FMMTaskService {
    public static final String EX_MSG_TAG_DATA_IS_MISSING = "One or more tag values are missing";

    private final ZifObjectModelService zifObjectModelService;
    private final VRFmmWebApiService vrFmmWebApiService;
    private final VRStorageService vrStorageService;
    private final VRAdaptationAndValidationService vrAdaptationAndValidationService;
    private final FMMMapper fmmMapper;
    private final VRCalcConfig config;

    public FMMTaskService(ZifObjectModelService zifObjectModelService,
                          VRFmmWebApiService vrFmmWebApiService,
                          VRStorageService vrStorageService,
                          VRAdaptationAndValidationService vrAdaptationAndValidationService,
                          FMMMapper fmmMapper, VRCalcConfig config) {
        this.zifObjectModelService = zifObjectModelService;
        this.vrFmmWebApiService = vrFmmWebApiService;
        this.vrStorageService = vrStorageService;
        this.vrAdaptationAndValidationService = vrAdaptationAndValidationService;
        this.fmmMapper = fmmMapper;
        this.config = config;
    }

    /**
     * Метод выполнения FMM задачи за указанную точку времени
     *
     * @param objectId      uid объекта модели (скважина)
     * @param date          метка времени UTC in ISO8601 format (2023-11-20T13:17:00.000Z)
     * @param logObjectName дополнительные сведения для операции логирования
     */
    public Flux<Void> executeFMMTask(String objectId, String date, String logObjectName) {
        return executeFMMTask(getRequestDataFMMTask(objectId, date), objectId, date, logObjectName);
    }

    protected Flux<Void> executeFMMTask(Mono<FMMTaskDto> fmmTaskDto, String objectId, String date, String logObjectName) {
        return vrFmmWebApiService.execFmmTaskNS(fmmTaskDto, logObjectName)
                .flatMapMany(solution -> saveFMMSolutionToPlatformWithCanonicalFactor(
                        solution.getSolution(),
                        objectId,
                        date,
                        logObjectName))
                .flatMap(res -> Flux.empty());
    }

    /**
     * Метод выполнения FMM задачи за промежуток времени. Задачи выполняются последовательно
     *
     * @param objectId          uid объекта модели (скважина)
     * @param timeLeft          левая граница времени UTC in ISO8601 format (2023-11-20T13:17:00.000Z)
     * @param timeRight         правая граница времени  UTC in ISO8601 format (2023-11-20T13:17:00.000Z)
     * @param logAdditionalData дополнительные сведения для операции логирования
     */
    public Mono<Void> executeFMMTaskDuration(String objectId, String timeLeft, String timeRight,
                                             String logAdditionalData) {
        return Flux.fromIterable(TimeUtils.getTimeList(timeLeft, timeRight))
                .publishOn(Schedulers.boundedElastic())
                .concatMap(date -> Mono.just(saveExecuteFMMTaskWithValidationAndBlock(
                        objectId,
                        date,
                        logAdditionalData)))
                .then();
    }

    /**
     * Метод выполнения FMM задачи за промежуток времени. Задачи выполняются параллельно.
     * Задачи выполняются последовательно группами по n штук, где n - количество ядер процессора.
     * Для каждой группы выполняется расчет валидации за время timeLeft первой задачи - количество дней валидации и
     * timeRight последней задачи. Таким образом данные валидации будут запрашиваться для каждой задачи только один
     * раз. Далее 5 задач выполняются параллельно.
     *
     * @param objectId          uid объекта модели (скважина)
     * @param timeLeft          левая граница времени UTC in ISO8601 format (2023-11-20T13:17:00.000Z)
     * @param timeRight         правая граница времени  UTC in ISO8601 format (2023-11-20T13:17:00.000Z)
     * @param logAdditionalData дополнительные сведения для операции логирования
     */
    public Mono<Void> executeFMMTaskDurationParallel(String objectId, String timeLeft, String timeRight,
                                                     String logAdditionalData) {
        return Flux.fromIterable(TimeUtils.getTimeList(timeLeft, timeRight))
                .window(config.getCores())
                .publishOn(Schedulers.boundedElastic())
                .concatMap(dateShortFlux -> executeFMMTaskShortDurationParallel(
                        dateShortFlux,
                        objectId,
                        logAdditionalData))
                .then();
    }

    private Flux<String> executeFMMTaskShortDurationParallel(Flux<String> datesFmmTask, String objectId,
                                                             String logAdditionalData) {
        return datesFmmTask
                .collectList()
                .flatMap(dateList -> vrAdaptationAndValidationService.executeTaskValidation(
                        objectId,
                        getTimeMinusDays(dateList.getFirst(), config.getVrTaskValidationCountDays()),
                        dateList.getLast()).thenReturn(dateList))
                .flatMapMany(Flux::fromIterable)
                .parallel(config.getCores())
                .runOn(Schedulers.boundedElastic())
                .map(date -> saveExecuteFMMTaskWithBlock(objectId, date, logAdditionalData))
                .sequential();
    }

    private String saveExecuteFMMTaskWithValidationAndBlock(String objectId, String date, String logAdditionalData) {
        try {
            vrAdaptationAndValidationService
                    .executeTaskValidation(
                            objectId,
                            getTimeMinusDays(date, config.getVrTaskValidationCountDays()),
                            date)
                    .then(executeFMMTask(objectId, date, logAdditionalData).collectList())
                    .block();
        } catch (Exception ex) {
            log.error("FM task error for time: " + date + " Error msg: " + ex.getMessage());
        }
        return date;
    }

    private String saveExecuteFMMTaskWithBlock(String objectId, String date, String logAdditionalData) {
        try {
            executeFMMTask(objectId, date, logAdditionalData).blockLast();
        } catch (Exception ex) {
            log.error("FM task error for time: " + date + " Error msg: " + ex.getMessage());
        }
        return date;
    }

    protected Flux<OmPropertyDto> getFMMObjectProperty(String parentObjectId, String logObjectName) {
        return vrAdaptationAndValidationService
                .getObjectByObjectParentId(parentObjectId, ZIF_FMM_OBJECT_NAME, logObjectName)
                .flatMapMany(omObjectDto ->
                        zifObjectModelService.getObjectPropertiesByObjectIdInFlux(omObjectDto.id(), logObjectName));
    }

    protected Flux<RtvInfoDto[]> saveFMMSolutionToPlatformWithCanonicalFactor(
            FMMSolutionDto fmmSolutionDto, String parentObjectId, String date, String logObjectName) {
        return getFMMObjectProperty(parentObjectId, logObjectName)
                .filter(omPropertyDto -> getFmmTaskResponseTagsNameList().contains(omPropertyDto.name().toLowerCase()))
                .flatMap(omPropertyDto -> vrAdaptationAndValidationService
                        .saveTagToPlatform(
                                omPropertyDto,
                                date,
                                getValueByPropertyAndFMMSolutionDto(omPropertyDto, fmmSolutionDto),
                                "FMM MODEL - " + logObjectName,
                                true));
    }

    private static Double getValueByPropertyAndFMMSolutionDto(OmPropertyDto omPropertyDto,
                                                              FMMSolutionDto fmmSolutionDto) {
        if (omPropertyDto.name().equalsIgnoreCase(ZIF_FMM_TAG_NAME_Q_GAS)) {
            return fmmSolutionDto.qGas();
        }
        if (omPropertyDto.name().equalsIgnoreCase(ZIF_FMM_TAG_NAME_Q_GC)) {
            return fmmSolutionDto.qGc();
        }
        if (omPropertyDto.name().equalsIgnoreCase(ZIF_FMM_TAG_NAME_Q_WAT)) {
            return fmmSolutionDto.qWat();
        }
        if (omPropertyDto.name().equalsIgnoreCase(ZIF_FMM_TAG_NAME_ERROR_GAS)) {
            return fmmSolutionDto.errorGas();
        }
        if (omPropertyDto.name().equalsIgnoreCase(ZIF_FMM_TAG_NAME_ERROR_GC)) {
            return fmmSolutionDto.errorGc();
        }
        if (omPropertyDto.name().equalsIgnoreCase(ZIF_FMM_TAG_NAME_ERROR_WAT)) {
            return fmmSolutionDto.errorWat();
        }
        return null;
    }

    protected Mono<FMMTaskDto> getRequestDataFMMTask(String objectId, String date) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        Mono<List<TagDataPointTimeDto>> tagAdditionalData = vrStorageService
                .getAdditionalObjectByNameAndMainObject(objectId, ZIF_MANIFOLD_OBJECT_NAME.toLowerCase())
                .zipWith(vrZifMainObject)
                .flatMap(tuple -> vrAdaptationAndValidationService.getTagDataByObjectId(
                        tuple.getT1().getZifUid(),
                        date,
                        tuple.getT2().getName(),
                        vrAdaptationAndValidationService.getVRAdditionalTagsNameList(),
                        true));
        Mono<List<TagDataPointTimeDto>> tagData = vrZifMainObject
                .flatMap(mainObject -> vrAdaptationAndValidationService.getTagDataByObjectId(
                        objectId,
                        date,
                        mainObject.getName(),
                        vrAdaptationAndValidationService.getFMMAndAdaptMainTagsNameList(),
                        true));
        Mono<ConstDataDto> constData = vrAdaptationAndValidationService.getConstDataByObjectParentId(objectId);
        Mono<VRValidationData> vrValidationData = vrZifMainObject.flatMap(mainObject ->
                vrStorageService.findValidationData(objectId, mainObject.getName()));
        Mono<VRAdaptationData> vrAdaptationData = vrStorageService.findActiveAdaptationDataByObjectId(objectId);

        return Mono.just(new FMMTaskDto())
                .zipWith(constData)
                .map(tuple -> fmmMapper.toFMMTaskDtoConst(tuple.getT1(), tuple.getT2()))
                .zipWith(tagAdditionalData)
                .flatMap(tuple -> mapperTagDataToFMMTask(tuple.getT1(), tuple.getT2()))
                .zipWith(tagData)
                .flatMap(tuple -> mapperTagDataToFMMTask(tuple.getT1(), tuple.getT2()))
                .zipWith(vrValidationData)
                .map(tuple -> fmmMapper.toFMMTaskDtoValidation(tuple.getT1(), tuple.getT2()))
                .zipWith(vrAdaptationData)
                .map(tuple -> fmmMapper.toFMMTaskDtoAdaptation(tuple.getT1(), tuple.getT2()));
    }

    private Mono<FMMTaskDto> mapperTagDataToFMMTask(FMMTaskDto fmmTaskDto,
                                                    List<TagDataPointTimeDto> tagDataPointTimeList) {
        if (tagDataPointTimeList.isEmpty()) {
            return Mono.error(new VRNotFoundApiException(EX_MSG_TAG_DATA_IS_MISSING));
        }
        for (TagDataPointTimeDto tag : tagDataPointTimeList) {
            switch (tag.getName().toLowerCase()) {
                case ZIF_MANIFOLD_TAG_NAME_P_OUT:
                    fmmTaskDto.setPOut(tag.getValue());
                    fmmTaskDto.getDataForMLWithoutCF().setPOut(tag.getValueWithoutCF());
                    break;
                case ZIF_MANIFOLD_TAG_NAME_T_OUT:
                    fmmTaskDto.setTOut(tag.getValue());
                    fmmTaskDto.getDataForMLWithoutCF().setTOut(tag.getValueWithoutCF());
                    break;
                case ZIF_TAG_NAME_D_CHOKE_PERCENT_TIMED:
                    fmmTaskDto.setDChokePercent(tag.getValue());
                    fmmTaskDto.getDataForMLWithoutCF().setDChokePercent(tag.getValueWithoutCF());
                    break;
                case ZIF_TAG_NAME_Q_GAS:
                    fmmTaskDto.setQGas(tag.getValue());
                    fmmTaskDto.getDataForMLWithoutCF().setQGas(tag.getValueWithoutCF());
                    break;
                case ZIF_TAG_NAME_T_BUF:
                    fmmTaskDto.setTBuf(tag.getValue());
                    fmmTaskDto.getDataForMLWithoutCF().setTBuf(tag.getValueWithoutCF());
                    break;
                case ZIF_TAG_NAME_P_BUF:
                    fmmTaskDto.setPBuf(tag.getValue());
                    fmmTaskDto.getDataForMLWithoutCF().setPBuf(tag.getValueWithoutCF());
                    break;
                case ZIF_TAG_NAME_Q_GS:
                    fmmTaskDto.setQGc(tag.getValue());
                    break;
                case ZIF_TAG_NAME_Q_WAT:
                    fmmTaskDto.setQWat(tag.getValue());
                    break;
                default:
            }
        }
        return Mono.just(fmmTaskDto);
    }

    private List<String> getFmmTaskResponseTagsNameList() {
        return new ArrayList<>(Arrays.asList(
                ZIF_FMM_TAG_NAME_Q_GAS.toLowerCase(),
                ZIF_FMM_TAG_NAME_Q_GC.toLowerCase(),
                ZIF_FMM_TAG_NAME_Q_WAT.toLowerCase(),
                ZIF_FMM_TAG_NAME_ERROR_GAS.toLowerCase(),
                ZIF_FMM_TAG_NAME_ERROR_GC.toLowerCase(),
                ZIF_FMM_TAG_NAME_ERROR_WAT.toLowerCase()
        ));
    }
}