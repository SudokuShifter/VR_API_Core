package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.*;
import com.inlinegroup.vrcalculationbackend.api.fmm.*;
import com.inlinegroup.vrcalculationbackend.api.zif.om.OmObjectDto;
import com.inlinegroup.vrcalculationbackend.api.zif.om.OmPropertyDto;
import com.inlinegroup.vrcalculationbackend.api.zif.udl.RtvInfoDto;
import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import com.inlinegroup.vrcalculationbackend.exceptions.ErrorMsgDataObject;
import com.inlinegroup.vrcalculationbackend.exceptions.VRApiException;
import com.inlinegroup.vrcalculationbackend.exceptions.VRNotFoundApiException;
import com.inlinegroup.vrcalculationbackend.mapper.VAMapper;
import com.inlinegroup.vrcalculationbackend.model.VRAdaptationData;
import com.inlinegroup.vrcalculationbackend.model.VRValidationData;
import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;
import com.inlinegroup.vrcalculationbackend.service.storage.ValidationDataStorage;
import com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.*;

@Service
@Slf4j
public class VRAdaptationAndValidationService {

    public static final String EX_MSG_TAG_DATA_IS_MISSING = "One or more tag values are missing";

    private final ZifObjectModelService zifObjectModelService;
    private final CanonicalFactorService canonicalFactorService;
    private final ZifUdlDfaWebApiService zifUdlDfaWebApiService;
    private final VRFmmWebApiService vrFmmWebApiService;
    private final VRStorageService vrStorageService;
    private final ValidationDataStorage validationDataStorage;
    private final VAMapper vaMapper;
    private final VRCalcConfig config;

    public VRAdaptationAndValidationService(ZifObjectModelService zifObjectModelService,
                                            CanonicalFactorService canonicalFactorService,
                                            ZifUdlDfaWebApiService zifUdlDfaWebApiService,
                                            VRFmmWebApiService vrFmmWebApiService,
                                            VRStorageService vrStorageService,
                                            ValidationDataStorage validationDataStorage,
                                            VAMapper vaMapper, VRCalcConfig config) {
        this.zifObjectModelService = zifObjectModelService;
        this.canonicalFactorService = canonicalFactorService;
        this.zifUdlDfaWebApiService = zifUdlDfaWebApiService;
        this.vrFmmWebApiService = vrFmmWebApiService;
        this.vrStorageService = vrStorageService;
        this.validationDataStorage = validationDataStorage;
        this.vaMapper = vaMapper;
        this.config = config;
    }

    /**
     * Метод получения данных валидации из базы данных
     *
     * @param objectId uid объекта модели (скважина)
     * @return VRValidationRecordResponse
     */
    public Mono<VRValidationRecordResponse> getValidationDataFromDB(String objectId) {
        return vrStorageService.findValidationData(objectId)
                .map(vrValidationData -> vaMapper.toValidationResponse(vrValidationData, objectId));
    }

    /**
     * Метод получения данных валидации из базы данных
     *
     * @param objectId            uid объекта модели (скважина)
     * @param isUserValue         активация пользовательских значений валидации в FMM задаче
     * @param wct                 значение wct, может быть null
     * @param gasCondensateFactor значение gasCondensateFactor, может быть null
     */
    public Mono<Void> setValidationData(String objectId, Boolean isUserValue,
                                        Double wct, Double gasCondensateFactor) {
        return vrStorageService.findValidationData(objectId)
                .flatMap(vrValidationData -> {
                    saveValidationData(vrValidationData, isUserValue, wct, gasCondensateFactor);
                    return Mono.empty();
                });
    }

    /**
     * Метод получение всех результатов расчетов адаптации из базы данных.
     *
     * @param objectId uid объекта модели (скважина)
     * @return VRAdaptationDataResponse
     */
    public Flux<VRAdaptationDataResponse> getAllAdaptationDataByObjectId(String objectId) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        return vrStorageService.findAllAdaptationDataByObjectId(objectId)
                .flatMap(adaptData -> vrZifMainObject.map(
                        mainObj -> vaMapper.toAdaptationResponse(adaptData, mainObj)));
    }

    /**
     * Метод получение активных результатов расчета адаптации из базы данных.
     *
     * @param objectId uid объекта модели (скважина)
     * @return VRAdaptationDataResponse
     */
    public Mono<VRAdaptationDataResponse> getActiveAdaptationDataByObjectId(String objectId) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        return vrStorageService.findActiveAdaptationDataByObjectId(objectId)
                .zipWith(vrZifMainObject)
                .map(tuple -> vaMapper.toAdaptationResponse(tuple.getT1(), tuple.getT2()));
    }

    /**
     * Метод активации расчета адаптации. Устанавливает id расчета в поле active_adaptation_value_id
     * таблицы vr_zif_objects.
     *
     * @param objectId  uid объекта модели (скважина)
     * @param adaptName имя расчета в базе данных
     */
    public Mono<Void> setActiveAdaptationDataByName(String objectId, String adaptName) {
        return vrStorageService.getObjectByUid(objectId)
                .zipWhen(mainObj -> vrStorageService
                        .findAdaptationDataByNameAndObjId(adaptName, mainObj.getId()), Tuples::of)
                .map(tuple -> {
                    tuple.getT1().setActiveAdaptationValueId(tuple.getT2().getId());
                    return tuple.getT1();
                })
                .flatMap(vrStorageService::saveMainObject)
                .flatMap(mainObj -> Mono.empty());
    }

    /**
     * Метод выполнения задачи адаптации и сохранения данных адаптации в базу данных.
     *
     * @param objectId  uid объекта модели (скважина)
     * @param dateLeft  метка времени начала выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param dateRight метка времени окончания выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param adaptName имя расчета в базе данных
     * @return VRAdaptationDataResponse
     */
    public Mono<VRAdaptationDataResponse> executeTaskAdaptationAndSave(String objectId, String dateLeft,
                                                                       String dateRight, String adaptName) {
        Mono<AdaptTaskDto> request = getRequestDataTaskAdaptation(objectId, dateLeft, dateRight);
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        return vrZifMainObject
                .flatMap(mainObject -> vrFmmWebApiService.execAdaptTaskNS(request, mainObject.getName()))
                .zipWith(vrZifMainObject)
                .map(tuple -> mapperToVRAdaptationData(tuple.getT1(), tuple.getT2(), adaptName, dateLeft, dateRight))
                .flatMap(vrStorageService::saveAdaptationData)
                .zipWith(vrZifMainObject)
                .map(tuple -> vaMapper.toAdaptationResponse(tuple.getT1(), tuple.getT2()));
    }

    /**
     * Метод выполнения задачи валидации. Данные валидации сохраняются в БД если значение isUserValue = false.
     * Если данные в таблице валидации отсутствуют, то создается новая запись.
     *
     * @param objectId  uid объекта модели (скважина)
     * @param dateStart метка времени начала выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param dateEnd   метка времени окончания выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @return VRValidationDataResponse
     */
    public Mono<VRValidationDataResponse> executeTaskValidation(String objectId, String dateStart, String dateEnd) {
        Mono<VRZifMainObject> vrZifObject = vrStorageService.getObjectByUid(objectId);
        Mono<VRValidationData> vrValidationData = vrStorageService.findValidationDataWithoutCheck(objectId)
                .switchIfEmpty(Mono.just(VRValidationData.builder().build()));
        Mono<TaskSolutionDto<ValidationSolutionDto>> validateTaskSolutionDto = vrZifObject
                .flatMap(mainObject -> vrFmmWebApiService.execValidateTaskNS(
                        getRequestDataTaskValidate(objectId, dateStart, dateEnd), mainObject.getName()));
        Mono<List<OmPropertyDto>> fmmPropertyDto = vrZifObject.flatMapMany(mainObject ->
                        getObjectByObjectParentId(objectId, ZIF_FMM_OBJECT_NAME, mainObject.getName())
                                .flatMapMany(omObjectDto -> zifObjectModelService
                                        .getObjectPropertiesByObjectIdInFlux(omObjectDto.id(), mainObject.getName())))
                .filter(omPropertyDto -> getValidationTagsNameList().contains(omPropertyDto.name().toLowerCase()))
                .collectList();
        return Mono.zip(validateTaskSolutionDto, vrZifObject, vrValidationData, fmmPropertyDto)
                .publishOn(Schedulers.boundedElastic())
                .map(tuple -> {
                    ValidationSolutionDto validationSolutionDto = tuple.getT1().getSolution();
                    List<OmPropertyDto> fmmPropertyList = tuple.getT4();
                    saveOrCreateValidationData(tuple.getT3(), tuple.getT2().getId(),
                            validationSolutionDto.wct(), validationSolutionDto.gasCondensateFactor());
                    for (OmPropertyDto omPropertyDto : fmmPropertyList) {
                        saveTagToPlatform(
                                omPropertyDto,
                                dateEnd,
                                getValueByPropertyAndValidateSolutionDto(omPropertyDto, validationSolutionDto),
                                "VALIDATION - " + tuple.getT2().getName(),
                                true).subscribe();
                    }
                    return vaMapper.toVrValidationDataResponse(tuple.getT1().getSolution(), tuple.getT2());
                });
    }

    /**
     * Метод получения тела запроса для задачи адаптации
     *
     * @param objectId  uid объекта модели (скважина)
     * @param dateLeft  метка времени начала выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param dateRight метка времени окончания выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @return AdaptTaskDto
     */
    protected Mono<AdaptTaskDto> getRequestDataTaskAdaptation(String objectId,
                                                              String dateLeft, String dateRight) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        Mono<List<TagDataTimeIntervalDto>> tagAdditionalDataTimeInterval = vrStorageService
                .getAdditionalObjectByNameAndMainObject(objectId, ZIF_MANIFOLD_OBJECT_NAME.toLowerCase())
                .flatMap(vrAdditionalObject -> getTagsDataByObjectId(
                        vrAdditionalObject.getZifUid(), dateLeft, dateRight, getVRAdditionalTagsNameList(),
                        vrZifMainObject, true));
        Mono<List<TagDataTimeIntervalDto>> tagDataTimeInterval = getTagsDataByObjectId(
                objectId, dateLeft, dateRight, getFMMAndAdaptMainTagsNameList(), vrZifMainObject, true);
        Mono<ConstDataDto> constData = getConstDataByObjectParentId(objectId);

        return Mono.just(new AdaptTaskDto())
                .zipWith(constData)
                .map(tuple -> vaMapper.toAdaptTaskDtoConst(tuple.getT1(), tuple.getT2()))
                .zipWith(tagAdditionalDataTimeInterval)
                .flatMap(tuple -> mapperTagDataToAdaptTask(tuple.getT1(), tuple.getT2()))
                .zipWith(tagDataTimeInterval)
                .flatMap(tuple -> mapperTagDataToAdaptTask(tuple.getT1(), tuple.getT2()));
    }

    /**
     * Метод получения тела запроса для задачи валидации
     *
     * @param objectId  uid объекта модели (скважина)
     * @param dateStart метка времени начала выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param dateEnd   метка времени окончания выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @return ValidateTaskDto
     */
    protected Mono<ValidateTaskDto> getRequestDataTaskValidate(String objectId, String dateStart, String dateEnd) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        return validationDataStorage
                .getTimeIntervalMono(objectId, dateStart, dateEnd)
                .flatMap(timeInterval -> {
                    if (Boolean.FALSE.equals(timeInterval.isRequest())) {
                        return Mono.just(new ArrayList<TagDataTimeIntervalDto>());
                    }
                    return getTagsDataByObjectId(objectId,
                            TimeUtils.toString(timeInterval.timeStart()),
                            TimeUtils.toString(timeInterval.timeEnd()),
                            getValidateTagsNameList(),
                            vrZifMainObject, true);
                })
                .flatMap(tagDataTimeIntervalDtoList -> validationDataStorage
                        .getAndSaveTagsDataMono(objectId, dateStart, dateEnd, tagDataTimeIntervalDtoList))
                .flatMap(this::mapperTagListToValidateTaskDto);
    }

    /**
     * Метод получения данных по тегам за промежуток времени. Для объекта модели с objectId выполняется выборка
     * всех корневых свойств, которые содержатся в списке propertyList. Для каждого свойства выполняется получения
     * коэффициента и перевод значения в систему СИ (объект UOM).
     *
     * @param objectId  uid объекта модели (скважина)
     * @param dateStart метка времени начала выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param dateEnd   метка времени окончания выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @return List<TagDataTimeIntervalDto> список тегов объекта модели. Каждый тег - массив значений с
     * метками времени.
     */
    protected Mono<List<TagDataTimeIntervalDto>> getTagsDataByObjectId(String objectId, String dateStart,
                                                                       String dateEnd, List<String> propertyList,
                                                                       Mono<VRZifMainObject> vrZifMainObject,
                                                                       boolean withCanonicalFactor) {
        return vrZifMainObject.flatMap(mainObject -> zifObjectModelService
                .getObjectPropertiesByObjectIdInFlux(objectId, mainObject.getName())
                .filter(omPropertyDto -> propertyList.contains(omPropertyDto.name().toLowerCase()))
                .flatMap(omPropertyDto -> {
                    Mono<Double> canonicalFactor;
                    if (omPropertyDto.name().toLowerCase().contains(ZIF_TEMPERATURE_CUR_NAME.toLowerCase())) {
                        canonicalFactor = Mono.just(1.0);
                    } else {
                        canonicalFactor = canonicalFactorService.getCFV(omPropertyDto.uomId());
                    }
                    Mono<TagDataTimeIntervalDto> tagDataDtoFlux = zifUdlDfaWebApiService
                            .getRtdRawValues(omPropertyDto.id(), dateStart, dateEnd,
                                    mainObject.getName() + ".  Tag name: " + omPropertyDto.name())
                            .map(Arrays::asList)
                            .map(rtvInfoDtoList -> {
                                Collections.sort(rtvInfoDtoList);
                                return rtvInfoDtoList;
                            })
                            .filter(list -> !list.isEmpty())
                            .map(rtvInfoDtoList -> mapperDataToTagTimeIntervalDto(rtvInfoDtoList, omPropertyDto))
                            .flatMap(tagDataTimeIntervalDto ->
                                    checkTagDataTimeIntervalDto(tagDataTimeIntervalDto, dateStart, dateEnd));
                    return Mono.zip(tagDataDtoFlux, canonicalFactor)
                            .map(tuple -> {
                                TagDataTimeIntervalDto result = tuple.getT1();
                                Double cf = tuple.getT2();
                                if (!withCanonicalFactor) {
                                    return result;
                                }
                                log.debug("Canonical factor for property: " + omPropertyDto.name() +
                                        " Value: " + cf + " UID Property: " + omPropertyDto.id() +
                                        " UID uom: " + omPropertyDto.uomId());
                                if (omPropertyDto.name().toLowerCase().contains(ZIF_TEMPERATURE_CUR_NAME.toLowerCase())) {
                                    result.setValues(result.getValues().stream().map(v -> v + FACTOR_TEMPERATURE_KELVIN)
                                            .collect(Collectors.toCollection(LinkedList::new)));
                                } else {
                                    result.setValues(result.getValues().stream().map(v -> v * cf)
                                            .collect(Collectors.toCollection(LinkedList::new)));
                                }
                                return result;
                            });
                })
                .sort()
                .collectList());
    }

    /**
     * Метод получения данных по тегам за метку времени. Для объекта модели с objectId выполняется выборка
     * всех корневых свойств, которые содержатся в списке filter. Для каждого свойства выполняется получения
     * коэффициента и перевод значения в систему СИ (объект UOM).
     *
     * @param objectId uid объекта модели (скважина)
     * @param date     метка времени выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @return List<TagDataPointTimeDto> список тегов объекта модели
     */
    protected Mono<List<TagDataPointTimeDto>> getTagDataByObjectId(String objectId, String date, String logObjectName,
                                                                   List<String> filter, boolean withCanonicalFactor) {
        return zifObjectModelService.getObjectPropertiesByObjectIdInFlux(objectId, logObjectName)
                .filter(omPropertyDto -> filter.contains(omPropertyDto.name().toLowerCase()))
                .flatMap(omPropertyDto -> {
                    Mono<Double> canonicalFactor;
                    if (omPropertyDto.name().toLowerCase().contains(ZIF_TEMPERATURE_CUR_NAME.toLowerCase())) {
                        canonicalFactor = Mono.just(1.0);
                    } else {
                        canonicalFactor = canonicalFactorService.getCFV(omPropertyDto.uomId());
                    }
                    Mono<RtvInfoDto[]> rtvInfoDto = zifUdlDfaWebApiService
                            .getRtdRawValue(omPropertyDto.id(), date, logObjectName +
                                    ". Tag name: " + omPropertyDto.name())
                            .switchIfEmpty(Mono.error(new VRNotFoundApiException("Tag value not found. Tag " +
                                    "name: " + omPropertyDto.name() + " Property UID: " + omPropertyDto.id())));
                    Mono<TagDataPointTimeDto> tagDataDto = rtvInfoDto
                            .map(rtv -> rtv[0])
                            .switchIfEmpty(Mono.error(new VRNotFoundApiException("Tag value not found. Tag " +
                                    "name: " + omPropertyDto.name() + " Property UID: " + omPropertyDto.id())))
                            .map(rtv -> mapperDataToTagPointTimeDto(rtv, omPropertyDto));
                    return Mono.zip(tagDataDto, canonicalFactor)
                            .map(tuple -> {
                                TagDataPointTimeDto result = tuple.getT1();
                                Double cf = tuple.getT2();
                                if (!withCanonicalFactor) {
                                    return result;
                                }
                                log.debug("Canonical factor for property: " + omPropertyDto.name() +
                                        " Value: " + cf + " UID Property: " + omPropertyDto.id() +
                                        " UID uom: " + omPropertyDto.uomId());
                                if (omPropertyDto.name().toLowerCase().contains(ZIF_TEMPERATURE_CUR_NAME.toLowerCase())) {
                                    result.setValue(result.getValue() + FACTOR_TEMPERATURE_KELVIN);
                                } else {
                                    result.setValue(result.getValue() * cf);
                                }
                                return result;
                            });
                })
                .collectList();
    }

    /**
     * Метод проверки валидности тегов для объекта типа "скважина". Запрашивает все ближайшие следующие значения тегов
     * скважины из списка getAllTagsList() за время curDate. Метод запроса тега NEXT. Проверяет валидность тегов
     * путем валидации значения и его даты. Дата не должна быть больше запрошенной + параметр
     * "vr-task-scheduler-check-step-after"
     *
     * @return Boolean
     */
    protected Boolean isValidTagsNext(String objectId, LocalDateTime curDate, String logObjectName) {
        return zifObjectModelService.getObjectPropertiesByObjectIdInFlux(objectId, logObjectName)
                .filter(omPropertyDto -> getAllTagsList().contains(omPropertyDto.name().toLowerCase()))
                .flatMap(omPropertyDto -> zifUdlDfaWebApiService
                        .getRtdRawNextValue(
                                omPropertyDto.id(),
                                TimeUtils.toString(curDate),
                                true,
                                logObjectName + ". Tag name:  " + omPropertyDto.name())
                        .switchIfEmpty(Mono.error(new VRNotFoundApiException(" Tag value not found. Tag " +
                                "name:  " + omPropertyDto.name() + " Property UID:  " + omPropertyDto.id()))))
                .collectList()
                .map(rtvInfoDtoList -> isValidateTagList(rtvInfoDtoList, curDate))
                .block();
    }

    private Boolean isValidateTagList(List<RtvInfoDto[]> tags, LocalDateTime curDate) {
        LocalDateTime checkDate = curDate
                .plusSeconds(config.getVrTaskSchedulerCheckStepAfter() * (long) TIME_STEP);
        int falseCount = 0;
        for (RtvInfoDto[] rtvInfoDto : tags) {
            if (rtvInfoDto.length == 0) {
                falseCount++;
                continue;
            }
            if (rtvInfoDto[0].getValue() == null || rtvInfoDto[0].getValue().isEmpty() ||
                    rtvInfoDto[0].getTime() == null || rtvInfoDto[0].getTime().isEmpty() ||
                    TimeUtils.getFrom(rtvInfoDto[0].getTime()).isAfter(checkDate)) {
                falseCount++;
            }
        }
        return falseCount <= DEFAULT_COUNT_BAD_TAG_VALUE_FOR_MAIN_OBJECT;
    }

    /**
     * Метод получения константных данных. Имя объекта константных данных содержит ZIF_CONST_DATA_OBJECT_NAME.
     * Объект с данными констант должен быть вложен в объект скважины.
     *
     * @param parentObjectId uid объекта модели (скважина)
     * @return ConstDataDto объект со значениями констант
     */
    protected Mono<ConstDataDto> getConstDataByObjectParentId(String parentObjectId) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(parentObjectId);
        Flux<TagDataPointTimeDto> tagsData =
                vrZifMainObject.flatMapMany(mainObject -> getObjectByObjectParentId(
                        parentObjectId, ZIF_CONST_DATA_OBJECT_NAME, mainObject.getName())
                        .flatMapMany(omObjectDto -> zifObjectModelService
                                .getObjectPropertiesByObjectIdInFlux(omObjectDto.id(), mainObject.getName()))
                        .flatMap(omPropertyDto -> zifUdlDfaWebApiService
                                .getRtdRawValue(omPropertyDto.id(),
                                        TimeUtils.getCurrentTimeMinusRound(), mainObject.getName() +
                                                ". Tag name: " + omPropertyDto.name())
                                .flatMapMany(Flux::fromArray)
                                .next()
                                .switchIfEmpty(Mono.error(new VRNotFoundApiException(
                                        "Tag with ID: " + omPropertyDto.id() + " is missing")))
                                .map(rtvInfoDto -> mapperDataToTagPointTimeDto(rtvInfoDto, omPropertyDto))));
        return tagsData
                .collectList()
                .flatMap(this::mapperTagListToConstDataDto);
    }

    /**
     * Метод получения вложенного объекта по его имени
     *
     * @param parentObjectId uid объекта модели (скважина)
     * @param objectName     имя вложенного объекта или его часть
     * @return OmObjectDto
     */
    protected Mono<OmObjectDto> getObjectByObjectParentId(String parentObjectId, String objectName,
                                                          String logObjectName) {
        return zifObjectModelService.getObjectById(parentObjectId, logObjectName)
                .flatMap(obj -> zifObjectModelService.getIncludeObjectsByModelId(obj.modelId(), obj.id(), logObjectName))
                .mapNotNull(omObjectPageDto -> {
                    Optional<OmObjectDto> result = omObjectPageDto.content().stream()
                            .filter(omObjectDto -> omObjectDto.name().toLowerCase()
                                    .contains(objectName.toLowerCase()))
                            .findFirst();
                    return result.orElse(null);
                })
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(
                        "The object was not found that name equals : " + objectName)));
    }

    /**
     * Метод выполняет сопоставление значений тегов временных рядов в объект TagDataTimeIntervalDto.
     * Т.к. на платформе Цифра значения по тегам могут выгружаться в обратном порядке, метод выполняет проверку
     * и при необходимости реверс коллекций со значениями тегов. Значения времени также присваиваются в обратном
     * порядке. Реверс выполняется за O(1)
     *
     * @param rtvInfoDtoList объект со значением тега
     * @param omPropertyDto  объект со значениями свойств объекта (скважины)
     * @return TagDataTimeIntervalDto
     */
    private TagDataTimeIntervalDto mapperDataToTagTimeIntervalDto(
            List<RtvInfoDto> rtvInfoDtoList, OmPropertyDto omPropertyDto) {
        TagDataTimeIntervalDto tagDataTimeIntervalDto = TagDataTimeIntervalDto.builder()
                .name(omPropertyDto.name())
                .id(omPropertyDto.id())
                .timeStart(TimeUtils.getFrom(rtvInfoDtoList.getFirst().getTime()))
                .timeEnd(TimeUtils.getFrom(rtvInfoDtoList.getLast().getTime()))
                .values(rtvInfoDtoList.stream()
                        .map(rtvInfoDto -> Double.valueOf(rtvInfoDto.getValue()))
                        .collect(Collectors.toCollection(LinkedList::new)))
                .timestamps(rtvInfoDtoList.stream().map(RtvInfoDto::getTime)
                        .collect(Collectors.toCollection(LinkedList::new)))
                .build();
        if (tagDataTimeIntervalDto.getTimeStart().isAfter(tagDataTimeIntervalDto.getTimeEnd())) {
            tagDataTimeIntervalDto.setTimeStart(TimeUtils.getFrom(rtvInfoDtoList.getLast().getTime()));
            tagDataTimeIntervalDto.setTimeEnd(TimeUtils.getFrom(rtvInfoDtoList.getFirst().getTime()));
            tagDataTimeIntervalDto.setValues(tagDataTimeIntervalDto.getValues().reversed());
            tagDataTimeIntervalDto.setTimestamps(tagDataTimeIntervalDto.getTimestamps().reversed());
        }
        return tagDataTimeIntervalDto;
    }

    private Mono<TagDataTimeIntervalDto> checkTagDataTimeIntervalDto(
            TagDataTimeIntervalDto tagDataTimeIntervalDto, String timeStart, String timeEnd) {
        if (tagDataTimeIntervalDto != null &&
                tagDataTimeIntervalDto.getTimeStart().equals(TimeUtils.getFrom(timeStart)) &&
                tagDataTimeIntervalDto.getTimeEnd().equals(TimeUtils.getFrom(timeEnd)) &&
                !tagDataTimeIntervalDto.getValues().isEmpty() &&
                !tagDataTimeIntervalDto.getTimestamps().isEmpty()) {
            return Mono.just(tagDataTimeIntervalDto);
        }
        ErrorMsgDataObject errorMsgDataObject = getErrorMsgDataObject(tagDataTimeIntervalDto, timeStart, timeEnd);
        log.error(errorMsgDataObject.toString());
        return Mono.error(new VRApiException("The values of the time range are not equal to the specified ones or " +
                "there is no data in the platform (Цифра)", errorMsgDataObject));
    }

    private static ErrorMsgDataObject getErrorMsgDataObject(TagDataTimeIntervalDto tagDataTimeIntervalDto,
                                                            String timeStart, String timeEnd) {
        ErrorMsgDataObject errorMsgDataObject = new ErrorMsgDataObject();
        if (tagDataTimeIntervalDto != null) {
            errorMsgDataObject.addMsg("Property id: " + tagDataTimeIntervalDto.getId());
            errorMsgDataObject.addMsg("Property name: " + tagDataTimeIntervalDto.getName());
            errorMsgDataObject.addMsg("Set value timeStart: " + timeStart + ", set value timeEnd: " + timeEnd +
                    ", Receive value timeStart from platform: " + tagDataTimeIntervalDto.getTimeStart() +
                    ", Receive value timeEnd from platform: " + tagDataTimeIntervalDto.getTimeEnd());
        }
        return errorMsgDataObject;
    }

    protected Mono<RtvInfoDto[]> saveTagToPlatform(OmPropertyDto omPropertyDto, String date,
                                                   Double value, String logObjectName,
                                                   boolean withCanonicalFactor) {
        if (value == null) {
            log.warn("The tag value is not recorded: " + omPropertyDto.name() +
                    "  Main Object UID: " + omPropertyDto.objectId());
            return Mono.empty();
        }
        Mono<Double> canonicalFactorValue;
        if (!withCanonicalFactor) {
            canonicalFactorValue = Mono.just(value);
        } else {
            if (omPropertyDto.name().toLowerCase().contains(ZIF_TEMPERATURE_CUR_NAME.toLowerCase())) {
                canonicalFactorValue = Mono.just(value + FACTOR_TEMPERATURE_KELVIN);
            } else {
                canonicalFactorValue = canonicalFactorService.getCFV(omPropertyDto.uomId())
                        .map(cf -> {
                            double val = value / cf;
                            log.debug("The tag value with canonical factor " + val + " prepare to write " +
                                    omPropertyDto.name() + " Main Object UID: " + omPropertyDto.objectId() +
                                    " Canonical factor value: " + cf);
                            return val;
                        });
            }
        }
        return canonicalFactorValue.flatMap(cfv -> zifUdlDfaWebApiService
                .writeRtdRawValue(omPropertyDto.id(), date, Double.toString(cfv), logObjectName));
    }

    private static Double getValueByPropertyAndValidateSolutionDto(OmPropertyDto omPropertyDto,
                                                                   ValidationSolutionDto validationSolutionDto) {
        if (omPropertyDto.name().equalsIgnoreCase(ZIF_FMM_TAG_NAME_GAS_CONDENSATE_FACTOR)) {
            return validationSolutionDto.gasCondensateFactor();
        }
        if (omPropertyDto.name().equalsIgnoreCase(ZIF_FMM_TAG_NAME_WATER_CUT)) {
            return validationSolutionDto.wct();
        }
        return null;
    }

    protected TagDataPointTimeDto mapperDataToTagPointTimeDto(RtvInfoDto rtvInfoDto, OmPropertyDto omPropertyDto) {
        Double value = rtvInfoDto.getValue().isEmpty() ? 0 : Double.parseDouble(rtvInfoDto.getValue());
        return TagDataPointTimeDto.builder()
                .name(omPropertyDto.name())
                .id(omPropertyDto.id())
                .value(value)
                .valueWithoutCF(value)
                .timestamp(rtvInfoDto.getTime())
                .build();
    }

    private Mono<ConstDataDto> mapperTagListToConstDataDto(List<TagDataPointTimeDto> tagDataPointTimeDtoList) {
        ConstDataDto constDataDto = new ConstDataDto();
        for (TagDataPointTimeDto tag : tagDataPointTimeDtoList) {
            if (tag.getName().equalsIgnoreCase(ZIF_TAG_NAME_GAMMA_GAS)) {
                constDataDto.setGammaGas(tag.getValue());
            }
            if (tag.getName().equalsIgnoreCase(ZIF_TAG_NAME_GAMMA_GC)) {
                constDataDto.setGammaGs(tag.getValue());
            }
            if (tag.getName().equalsIgnoreCase(ZIF_TAG_NAME_GAMMA_WAT)) {
                constDataDto.setGammaWat(tag.getValue());
            }
            if (tag.getName().equalsIgnoreCase(ZIF_TAG_NAME_D_TUBE)) {
                constDataDto.setDTube(tag.getValue());
            }
        }
        if (constDataDto.getGammaGas() != null && constDataDto.getGammaGs() != null &&
                constDataDto.getGammaWat() != null && constDataDto.getDTube() != null) {
            return Mono.just(constDataDto);
        }
        return Mono.error(new VRNotFoundApiException(EX_MSG_TAG_DATA_IS_MISSING));
    }

    private Mono<AdaptTaskDto> mapperTagDataToAdaptTask(AdaptTaskDto adaptTaskDto,
                                                        List<TagDataTimeIntervalDto> tagDataTimeIntervalList) {
        if (isNotValidateTagsData(tagDataTimeIntervalList)) {
            return Mono.error(new VRNotFoundApiException(EX_MSG_TAG_DATA_IS_MISSING));
        }
        for (TagDataTimeIntervalDto tag : tagDataTimeIntervalList) {
            switch (tag.getName().toLowerCase()) {
                case ZIF_MANIFOLD_TAG_NAME_P_OUT:
                    adaptTaskDto.setPOutTimed(tag.getValues());
                    break;
                case ZIF_MANIFOLD_TAG_NAME_T_OUT:
                    adaptTaskDto.setTOutTimed(tag.getValues());
                    break;
                case ZIF_TAG_NAME_D_CHOKE_PERCENT_TIMED:
                    adaptTaskDto.setDChokePercentTimed(tag.getValues());
                    break;
                case ZIF_TAG_NAME_Q_GAS:
                    adaptTaskDto.setQGasTimed(tag.getValues());
                    break;
                case ZIF_TAG_NAME_T_BUF:
                    adaptTaskDto.setTBufTimed(tag.getValues());
                    break;
                case ZIF_TAG_NAME_P_BUF:
                    adaptTaskDto.setPBufTimed(tag.getValues());
                    break;
                case ZIF_TAG_NAME_Q_GS:
                    adaptTaskDto.setQGcTimed(tag.getValues());
                    break;
                case ZIF_TAG_NAME_Q_WAT:
                    adaptTaskDto.setQWatTimed(tag.getValues());
                    break;
                default:
            }
        }
        adaptTaskDto.setTimestamp(tagDataTimeIntervalList.get(0).getTimestamps());
        return Mono.just(adaptTaskDto);
    }

    private Mono<ValidateTaskDto> mapperTagListToValidateTaskDto(List<TagDataTimeIntervalDto> tagDataTimeIntervalDto) {
        if (isNotValidateTagsData(tagDataTimeIntervalDto)) {
            return Mono.error(new VRNotFoundApiException(EX_MSG_TAG_DATA_IS_MISSING));
        }
        ValidateTaskDto validateTaskDto = new ValidateTaskDto();
        for (TagDataTimeIntervalDto tag : tagDataTimeIntervalDto) {
            switch (tag.getName().toLowerCase()) {
                case ZIF_TAG_NAME_Q_GAS:
                    validateTaskDto.setQGasTimed(tag.getValues());
                    break;
                case ZIF_TAG_NAME_Q_GS:
                    validateTaskDto.setQGcTimed(tag.getValues());
                    break;
                case ZIF_TAG_NAME_Q_WAT:
                    validateTaskDto.setQWatTimed(tag.getValues());
                    break;
                default:
            }
        }
        return Mono.just(validateTaskDto);
    }

    private VRAdaptationData mapperToVRAdaptationData(
            TaskSolutionDto<AdaptSolutionDto> taskSolutionDto, VRZifMainObject vrZifMainObject, String adaptName,
            String dateLeft, String dateRight) {
        return VRAdaptationData.builder()
                .chokeAdaptValue(taskSolutionDto.getSolution().cChokeAdapt())
                .chokeAdaptPercent(taskSolutionDto.getSolution().dChokePercentAdapt())
                .creationDate(LocalDateTime.now())
                .vrZifObjectsId(vrZifMainObject.getId())
                .name(adaptName)
                .dateStart(TimeUtils.getFrom(dateLeft))
                .dateEnd(TimeUtils.getFrom(dateRight))
                .build();
    }

    private boolean isNotValidateTagsData(List<TagDataTimeIntervalDto> tagDataTimeIntervalList) {
        if (tagDataTimeIntervalList.isEmpty()) {
            log.warn("Tags values is missing");
            return true;
        }
        int stValue = tagDataTimeIntervalList.get(0).getValues().size();
        if (stValue == 0) {
            log.warn("List tag values is empty");
            return true;
        }
        for (TagDataTimeIntervalDto tag : tagDataTimeIntervalList) {
            if (tag.getValues().size() != stValue || tag.getTimestamps().size() != stValue) {
                log.warn("The sizes of tag list values are not equal each other");
                return true;
            }
        }
        return false;
    }

    protected List<String> getValidateTagsNameList() {
        return new ArrayList<>(Arrays.asList(
                ZIF_TAG_NAME_Q_GAS.toLowerCase(),
                ZIF_TAG_NAME_Q_GS.toLowerCase(),
                ZIF_TAG_NAME_Q_WAT.toLowerCase()
        ));
    }

    protected List<String> getVRAdditionalTagsNameList() {
        return new ArrayList<>(Arrays.asList(
                ZIF_MANIFOLD_TAG_NAME_P_OUT.toLowerCase(),
                ZIF_MANIFOLD_TAG_NAME_T_OUT.toLowerCase()
        ));
    }

    protected List<String> getFMMAndAdaptMainTagsNameList() {
        return new ArrayList<>(Arrays.asList(
                ZIF_TAG_NAME_D_CHOKE_PERCENT_TIMED.toLowerCase(),
                ZIF_TAG_NAME_Q_GAS.toLowerCase(),
                ZIF_TAG_NAME_Q_GS.toLowerCase(),
                ZIF_TAG_NAME_Q_WAT.toLowerCase(),
                ZIF_TAG_NAME_T_BUF.toLowerCase(),
                ZIF_TAG_NAME_P_BUF.toLowerCase()
        ));
    }

    protected List<String> getValidationTagsNameList() {
        return new ArrayList<>(Arrays.asList(
                ZIF_FMM_TAG_NAME_GAS_CONDENSATE_FACTOR.toLowerCase(),
                ZIF_FMM_TAG_NAME_WATER_CUT.toLowerCase()
        ));
    }

    protected List<String> getAllTagsList() {
        return new ArrayList<>(Arrays.asList(
                ZIF_TAG_NAME_D_CHOKE_PERCENT_TIMED.toLowerCase(),
                ZIF_TAG_NAME_Q_GAS.toLowerCase(),
                ZIF_TAG_NAME_Q_GS.toLowerCase(),
                ZIF_TAG_NAME_Q_WAT.toLowerCase(),
                ZIF_TAG_NAME_T_BUF.toLowerCase(),
                ZIF_TAG_NAME_P_BUF.toLowerCase(),
                ZIF_TAG_NAME_P_DOWN_HOLE.toLowerCase(),
                ZIF_TAG_NAME_T_DOWN_HOLE.toLowerCase(),
                ZIF_MANIFOLD_TAG_NAME_P_OUT.toLowerCase(),
                ZIF_MANIFOLD_TAG_NAME_T_OUT.toLowerCase()
        ));
    }

    private void saveOrCreateValidationData(VRValidationData vrValidationData, Long vrZifObjectsId,
                                            Double wct, Double gasCondensateFactor) {
        VRValidationData validationData;
        if (vrValidationData.getId() == null) {
            validationData = VRValidationData.builder()
                    .vrZifObjectsId(vrZifObjectsId)
                    .wct(wct)
                    .gasCondensateFactor(gasCondensateFactor)
                    .isUserValue(false)
                    .date(LocalDateTime.now())
                    .build();
        } else {
            validationData = vrValidationData;
            if (validationData.getIsUserValue() != null && !validationData.getIsUserValue()) {
                validationData.setWct(wct);
                validationData.setGasCondensateFactor(gasCondensateFactor);
                validationData.setDate(LocalDateTime.now());
            }
        }
        vrStorageService.saveValidationData(validationData).subscribe();
    }

    private void saveValidationData(VRValidationData vrValidationData, Boolean isUserValue,
                                    Double wct, Double gasCondensateFactor) {
        vrValidationData.setIsUserValue(isUserValue);
        vrValidationData.setWct(wct);
        vrValidationData.setGasCondensateFactor(gasCondensateFactor);
        vrValidationData.setDate(LocalDateTime.now());
        vrStorageService.saveValidationData(vrValidationData).subscribe();
    }
}