package com.inlinegroup.vrcalculationbackend.repositories;

import com.inlinegroup.vrcalculationbackend.model.VRValidationData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface VRValidationDataRepository extends ReactiveCrudRepository<VRValidationData, Long> {
    @Query("SELECT vrv.* FROM vr_validation_data vrv " +
            "LEFT JOIN vr_zif_objects vrz ON vrv.vr_zif_objects_id = vrz.id " +
            "WHERE vrz.zif_uid = :objectUid LIMIT 1")
    Mono<VRValidationData> findValidationData(String objectUid);
}
