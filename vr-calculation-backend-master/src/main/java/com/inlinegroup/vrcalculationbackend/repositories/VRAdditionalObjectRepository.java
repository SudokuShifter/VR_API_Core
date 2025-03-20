package com.inlinegroup.vrcalculationbackend.repositories;

import com.inlinegroup.vrcalculationbackend.model.VRZifAdditionalObject;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface VRAdditionalObjectRepository extends ReactiveCrudRepository<VRZifAdditionalObject, Long> {
    @Query("SELECT vrza.* FROM vr_zif_additional_objects vrza " +
            "LEFT JOIN vr_zif_object_2_additional_object vro2a ON vro2a.vr_zif_additional_objects_id = vrza.id " +
            "LEFT JOIN vr_zif_objects vrz ON vro2a.vr_zif_objects_id = vrz.id " +
            "WHERE vrz.zif_uid = :objectUid AND LOWER(vrza.name) LIKE '%' || :objectName || '%' LIMIT 1")
    Mono<VRZifAdditionalObject> findAdditionalObjectByMainObjectName(String objectUid, String objectName);
}
