package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.exceptions.VRNotFoundApiException;
import com.inlinegroup.vrcalculationbackend.model.*;
import com.inlinegroup.vrcalculationbackend.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VRStorageService {
    public static final String EX_MSG_DATA_IS_MISSING_IN_DB = "The data is missing from the database";
    private final VRAdaptationDataRepository vrAdaptationDataRepository;
    private final VRMainObjectRepository vrMainObjectRepository;
    private final VRAdditionalObjectRepository vrAdditionalObjectRepository;
    private final VRValidationDataRepository vrValidationDataRepository;
    private final VRTypeRepository vrTypeRepository;

    public VRStorageService(VRAdaptationDataRepository vrAdaptationDataRepository,
                            VRMainObjectRepository vrMainObjectRepository,
                            VRAdditionalObjectRepository vrAdditionalObjectRepository,
                            VRValidationDataRepository vrValidationDataRepository,
                            VRTypeRepository vrTypeRepository) {
        this.vrAdaptationDataRepository = vrAdaptationDataRepository;
        this.vrMainObjectRepository = vrMainObjectRepository;
        this.vrAdditionalObjectRepository = vrAdditionalObjectRepository;
        this.vrValidationDataRepository = vrValidationDataRepository;
        this.vrTypeRepository = vrTypeRepository;
    }

    @Transactional
    public Mono<VRAdaptationData> saveAdaptationData(VRAdaptationData vrAdaptationData) {
        return vrAdaptationDataRepository.save(vrAdaptationData);
    }

    @Transactional
    public Mono<VRValidationData> saveValidationData(VRValidationData vrValidationData) {
        return vrValidationDataRepository.save(vrValidationData);
    }

    @Transactional
    public Mono<VRZifMainObject> saveMainObject(VRZifMainObject vrZifMainObject) {
        return vrMainObjectRepository.save(vrZifMainObject);
    }

    public Flux<VRZifMainObject> getAllActiveObject() {
        return vrMainObjectRepository.findAllByActiveAdaptationValueIdNotNull()
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(EX_MSG_DATA_IS_MISSING_IN_DB)));
    }

    public Flux<VRZifMainObject> getAllMainObject() {
        return vrMainObjectRepository.findAll()
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(EX_MSG_DATA_IS_MISSING_IN_DB)));
    }

    public Mono<VRZifMainObject> getObjectByUid(String uid) {
        return vrMainObjectRepository.findVRZifObjectByZifUid(uid)
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(EX_MSG_DATA_IS_MISSING_IN_DB)));
    }

    public Mono<VRZifMainObject> getObjectById(Long id) {
        return vrMainObjectRepository.findById(id)
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(EX_MSG_DATA_IS_MISSING_IN_DB)));
    }

    public Mono<VRZifAdditionalObject> getAdditionalObjectByNameAndMainObject(String objectUid, String objectName) {
        return vrAdditionalObjectRepository.findAdditionalObjectByMainObjectName(objectUid, objectName)
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(EX_MSG_DATA_IS_MISSING_IN_DB)));
    }

    public Flux<VRAdaptationData> findAllAdaptationDataByObjectId(String objectId) {
        return vrAdaptationDataRepository.findAllAdaptationData(objectId)
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(EX_MSG_DATA_IS_MISSING_IN_DB)));
    }

    public Mono<VRAdaptationData> findActiveAdaptationDataByObjectId(String objectId) {
        return vrAdaptationDataRepository.findActiveAdaptationData(objectId)
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(EX_MSG_DATA_IS_MISSING_IN_DB)));
    }

    public Mono<VRAdaptationData> findAdaptationDataByNameAndObjId(String name, Long id) {
        return vrAdaptationDataRepository.findVRAdaptationDataByNameAndVrZifObjectsId(name, id)
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(EX_MSG_DATA_IS_MISSING_IN_DB)));
    }

    public Mono<VRValidationData> findValidationDataWithoutCheck(String uid) {
        return vrValidationDataRepository.findValidationData(uid);
    }

    public Mono<VRValidationData> findValidationData(String uid, String logAdditionalData) {
        return vrValidationDataRepository.findValidationData(uid)
                .switchIfEmpty(Mono.error(new VRNotFoundApiException("Validation data not found. " +
                        logAdditionalData + " Object uid: " + uid)));
    }

    public Mono<VRValidationData> findValidationData(String uid) {
        return findValidationData(uid, "");
    }

    public Flux<VRType> findAllVRType() {
        return vrTypeRepository.findAll()
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(EX_MSG_DATA_IS_MISSING_IN_DB)));
    }

    public Mono<VRType> findVRTypeByUid(String uid) {
        return vrTypeRepository.findById(uid)
                .switchIfEmpty(Mono.error(new VRNotFoundApiException(EX_MSG_DATA_IS_MISSING_IN_DB)));
    }

    public List<VRZifMainObject> getMainObjectList() {
        Optional<List<VRZifMainObject>> mainObjects = getAllActiveObject().collectList().blockOptional();
        return mainObjects.orElse(new ArrayList<>());
    }
}