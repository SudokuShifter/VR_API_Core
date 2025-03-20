package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.VRTypeCalculation;
import com.inlinegroup.vrcalculationbackend.mapper.VRAdditionalMapper;
import com.inlinegroup.vrcalculationbackend.model.VRType;
import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AdditionalFunctionalService {

    private final VRStorageService vrStorageService;
    private final VRAdditionalMapper vrAdditionalMapper;

    public AdditionalFunctionalService(VRStorageService vrStorageService, VRAdditionalMapper vrAdditionalMapper) {
        this.vrStorageService = vrStorageService;
        this.vrAdditionalMapper = vrAdditionalMapper;
    }

    public Flux<VRTypeCalculation> getAllTypeCalculation() {
        return vrStorageService.getAllMainObject()
                .map(vrAdditionalMapper::toVRTypeCalculation)
                .sort();
    }

    public Mono<VRTypeCalculation> getTypeCalculationById(String uid) {
        return vrStorageService.getObjectByUid(uid)
                .map(vrAdditionalMapper::toVRTypeCalculation);
    }

    public Mono<Void> setTypeCalculationValueById(String uid, String value) {
        Mono<String> vrType = vrStorageService.findVRTypeByUid(value)
                .map(VRType::getId);
        return vrStorageService.getObjectByUid(uid)
                .zipWith(vrType)
                .map(tuple -> {
                    VRZifMainObject mainObject = tuple.getT1();
                    mainObject.setActiveVrType(tuple.getT2());
                    return mainObject;
                })
                .flatMap(vrStorageService::saveMainObject)
                .then();
    }
}
