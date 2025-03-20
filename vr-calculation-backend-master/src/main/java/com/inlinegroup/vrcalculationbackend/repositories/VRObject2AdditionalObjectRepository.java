package com.inlinegroup.vrcalculationbackend.repositories;

import com.inlinegroup.vrcalculationbackend.model.VRZifObject2AdditionalObject;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VRObject2AdditionalObjectRepository
        extends ReactiveCrudRepository<VRZifObject2AdditionalObject, Long> {
}
