package com.inlinegroup.vrcalculationbackend.repositories;

import com.inlinegroup.vrcalculationbackend.model.VRAdaptationData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface VRAdaptationDataRepository extends ReactiveCrudRepository<VRAdaptationData, Long> {
    @Query("SELECT vra.* FROM vr_adaptation_data vra " +
            "LEFT JOIN vr_zif_objects vrz ON vra.vr_zif_objects_id = vrz.id " +
            "WHERE vra.id = vrz.active_adaptation_value_id AND vrz.zif_uid = :objectUid LIMIT 1")
    Mono<VRAdaptationData> findActiveAdaptationData(String objectUid);

    @Query("SELECT vra.* FROM vr_adaptation_data vra " +
            "LEFT JOIN vr_zif_objects vrz ON vra.vr_zif_objects_id = vrz.id " +
            "WHERE vrz.zif_uid = :objectUid")
    Flux<VRAdaptationData> findAllAdaptationData(String objectUid);

    Mono<VRAdaptationData> findVRAdaptationDataByNameAndVrZifObjectsId(String name, Long id);
}
