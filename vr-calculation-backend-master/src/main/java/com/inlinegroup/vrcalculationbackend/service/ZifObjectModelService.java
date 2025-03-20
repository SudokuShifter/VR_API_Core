package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.zif.enums.FilterMode;
import com.inlinegroup.vrcalculationbackend.api.zif.om.OmObjectDto;
import com.inlinegroup.vrcalculationbackend.api.zif.om.OmObjectPageDto;
import com.inlinegroup.vrcalculationbackend.api.zif.om.OmPropertyDto;
import com.inlinegroup.vrcalculationbackend.api.zif.om.OmPropertyPageDto;
import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import com.inlinegroup.vrcalculationbackend.exceptions.VRNotFoundApiException;
import com.inlinegroup.vrcalculationbackend.service.enums.AppType;
import com.inlinegroup.vrcalculationbackend.service.utils.ZifUri;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.*;

@Component
public class ZifObjectModelService {

    private static final String LOG_UID_TEXT = " UID: ";
    private final VRCalcConfig config;
    private final WebClientExecutionService webClientExecutionService;

    public ZifObjectModelService(VRCalcConfig config, WebClientExecutionService webClientExecutionService) {
        this.config = config;
        this.webClientExecutionService = webClientExecutionService;
    }

    /**
     * Метод получения всех объектов модели.
     * В случае получения статусов 404 или 410 будет выброшено исключение VRNotFoundClientException.
     * В случае получения статусов 4xx и 5xx будет выброшено исключение VRClientException.
     *
     * @param modelId             id модели
     * @param onlyRoot            получить вложенные элементы модели
     * @param includeDeleted      вложить удаленные объекты модели
     * @param calculateTotalCount выполнить подсчет общего количества объектов
     * @param logAdditionalData   дополнительные сведения для операции логирования
     * @return OmObjectPageDto
     */
    public Mono<OmObjectPageDto> getAllObjectsByModelId(String modelId, boolean onlyRoot, boolean includeDeleted,
                                                        boolean calculateTotalCount, String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config)
                .setPath(config.getUriOmObjectGetAllObjectsByModel())
                .addPathId(modelId)
                .addQueryParams(ZIF_OM_QP_ONLY_ROOT, String.valueOf(onlyRoot))
                .addQueryParams(ZIF_OM_QP_INCLUDE_DELETED, String.valueOf(includeDeleted))
                .addQueryParams(ZIF_OM_QP_CALCULATE_TOTAL_COUNT, String.valueOf(calculateTotalCount))
                .build();
        return webClientExecutionService
                .executeMonoGetRequest(uri.getUri(), AppType.ZIF, new ParameterizedTypeReference<>() {
                }, "Get all object. " + logAdditionalData + LOG_UID_TEXT + modelId);
    }

    public Mono<OmObjectPageDto> getAllObjectsByModelId(String modelId, String logAdditionalData) {
        return getAllObjectsByModelId(
                modelId, true, false, true, logAdditionalData);
    }

    public Mono<OmObjectPageDto> getAllObjectsByModelId(String modelId, boolean onlyRoot, String logAdditionalData) {
        return getAllObjectsByModelId(modelId, onlyRoot, false, true, logAdditionalData);
    }

    /**
     * Метод получения всех вложенных объектов модели.
     * В случае получения статусов 404 или 410 будет выброшено исключение VRNotFoundClientException.
     * В случае получения статусов 4xx и 5xx будет выброшено исключение VRClientException.
     *
     * @param modelId             id модели
     * @param objectId            id объекта, из которого получаем вложенные
     * @param filterMode          фильтр вывода объектов (ONLY_ELEMENT - без дочерних, WITH_PARENT - с дочерними)
     * @param includeDeleted      вложить удаленные объекты модели
     * @param calculateTotalCount выполнить подсчет общего количества объектов
     * @param logAdditionalData   дополнительные сведения для операции логирования
     * @return OmObjectPageDto
     */
    public Mono<OmObjectPageDto> getIncludeObjectsByModelId(String modelId, String objectId, FilterMode filterMode,
                                                            boolean includeDeleted, boolean calculateTotalCount,
                                                            String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config)
                .setPath(config.getUriOmObjectGetIncludeObjectsByModel())
                .addPathId(modelId)
                .addPathId(objectId)
                .addQueryParams(ZIF_OM_QP_INCLUDE_DELETED, String.valueOf(includeDeleted))
                .addQueryParams(ZIF_OM_QP_CALCULATE_TOTAL_COUNT, String.valueOf(calculateTotalCount))
                .addQueryParams(ZIF_OM_QP_FILTER_MODE, filterMode.getMode())
                .build();
        return webClientExecutionService
                .executeMonoGetRequest(uri.getUri(), AppType.ZIF, new ParameterizedTypeReference<>() {
                }, "Get include object. " + logAdditionalData + LOG_UID_TEXT + modelId);
    }

    public Mono<OmObjectPageDto> getIncludeObjectsByModelId(String modelId, String objectId, String logAdditionalData) {
        return getIncludeObjectsByModelId(modelId, objectId, FilterMode.ONLY_ELEMENT,
                false, true, logAdditionalData);
    }

    /**
     * Метод получения объекта по id.
     * В случае получения статусов 404 или 410 будет выброшено исключение VRNotFoundClientException.
     * В случае получения статусов 4xx и 5xx будет выброшено исключение VRClientException.
     *
     * @param id                id объекта
     * @param includeDeleted    вложить удаленные объекты модели
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return OmObjectDto
     */
    public Mono<OmObjectDto> getObjectById(String id, boolean includeDeleted, String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config)
                .setPath(config.getUriOmObjectGetObjectById())
                .addPathId(id)
                .addQueryParams(ZIF_OM_QP_INCLUDE_DELETED, String.valueOf(includeDeleted))
                .build();
        return webClientExecutionService
                .executeMonoGetRequest(uri.getUri(), AppType.ZIF, new ParameterizedTypeReference<>() {
                }, "Get object. " + logAdditionalData + LOG_UID_TEXT + id);
    }

    public Mono<OmObjectDto> getObjectById(String id, String logAdditionalData) {
        return getObjectById(id, false, logAdditionalData);
    }

    /**
     * Метод получения свойств по параметрам: ID объекта, имя.
     * В случае получения статусов 404 или 410 будет выброшено исключение VRNotFoundClientException.
     * В случае получения статусов 4xx и 5xx будет выброшено исключение VRClientException.
     *
     * @param objectId            id объекта, для которого получаем свойства
     * @param filterMode          фильтр вывода объектов (ONLY_ELEMENT - без дочерних, WITH_PARENT - с дочерними)
     * @param onlyRoot            получить вложенные элементы модели
     * @param includeDeleted      вложить удаленные объекты модели
     * @param calculateTotalCount выполнить подсчет общего количества объектов
     * @param logAdditionalData   дополнительные сведения для операции логирования
     * @return OmPropertyPageDto
     */
    public Mono<OmPropertyPageDto> getObjectProperties(String objectId, String name, FilterMode filterMode,
                                                       boolean onlyRoot, boolean includeDeleted,
                                                       boolean calculateTotalCount, String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config)
                .setPath(config.getUriOmObjectGetProperties())
                .addQueryParams(ZIF_OM_QP_OBJECT_ID, objectId)
                .addQueryParams(ZIF_OM_QP_NAME, name)
                .addQueryParams(ZIF_OM_QP_ONLY_ROOT, String.valueOf(onlyRoot))
                .addQueryParams(ZIF_OM_QP_INCLUDE_DELETED, String.valueOf(includeDeleted))
                .addQueryParams(ZIF_OM_QP_CALCULATE_TOTAL_COUNT, String.valueOf(calculateTotalCount))
                .addQueryParams(ZIF_OM_QP_FILTER_MODE, filterMode.getMode())
                .build();
        return webClientExecutionService.
                executeMonoGetRequest(uri.getUri(), AppType.ZIF, new ParameterizedTypeReference<>() {
                }, "Get all properties. " + logAdditionalData + LOG_UID_TEXT + objectId);
    }

    public Mono<OmPropertyPageDto> getObjectPropertiesByObjectID(String objectId, String logAdditionalData) {
        return getObjectProperties(objectId, "", FilterMode.ONLY_ELEMENT,
                true, false, true, logAdditionalData);
    }

    public Mono<OmPropertyPageDto> getObjectPropertiesByName(String name, String logAdditionalData) {
        return getObjectProperties("", name, FilterMode.ONLY_ELEMENT,
                true, false, true, logAdditionalData);
    }

    /**
     * Метод получения свойств объекта по его ID
     * В случае получения статусов 404 или 410 будет выброшено исключение VRNotFoundClientException.
     * В случае получения статусов 4xx и 5xx будет выброшено исключение VRClientException.
     *
     * @param objectId          id объекта модели
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return Flux<OmPropertyDto>
     */
    public Flux<OmPropertyDto> getObjectPropertiesByObjectIdInFlux(String objectId, String logAdditionalData) {
        return getObjectPropertiesByObjectID(objectId, logAdditionalData)
                .map(OmPropertyPageDto::content)
                .flatMapMany(Flux::fromIterable)
                .filter(omObjectDto -> !omObjectDto.isDeleted())
                .switchIfEmpty(Mono.error(new VRNotFoundApiException("The object has no properties. " +
                        LOG_UID_TEXT + objectId)));
    }

    /**
     * Метод получения свойства по его ID
     * В случае получения статусов 404 или 410 будет выброшено исключение VRNotFoundClientException.
     * В случае получения статусов 4xx и 5xx будет выброшено исключение VRClientException.
     *
     * @param includeDeleted    вложить удаленные объекты модели
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return OmObjectPageDto
     */
    public Mono<OmPropertyDto> getObjectProperty(String propertyId, boolean includeDeleted, String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config)
                .setPath(config.getUriOmObjectGetProperty())
                .addPathId(propertyId)
                .addQueryParams(ZIF_OM_QP_INCLUDE_DELETED, String.valueOf(includeDeleted))
                .build();
        return webClientExecutionService.
                executeMonoGetRequest(uri.getUri(), AppType.ZIF, new ParameterizedTypeReference<>() {
                }, "Get property. " + logAdditionalData + " Property UID: " + propertyId);
    }

    public Mono<OmPropertyDto> getObjectProperty(String propertyId, String logAdditionalData) {
        return getObjectProperty(propertyId, false, logAdditionalData);
    }
}