package com.inlinegroup.vrcalculationbackend.repositories;

import com.inlinegroup.vrcalculationbackend.model.VRType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VRTypeRepository extends ReactiveCrudRepository<VRType, String> {
}
