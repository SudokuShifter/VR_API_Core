package com.inlinegroup.vrcalculationbackend.repositories;

import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface VRMainObjectRepository extends ReactiveCrudRepository<VRZifMainObject, Long> {
    Mono<VRZifMainObject> findVRZifObjectByZifUid(String zifUid);
    Flux<VRZifMainObject> findAllByActiveAdaptationValueIdNotNull();
}
