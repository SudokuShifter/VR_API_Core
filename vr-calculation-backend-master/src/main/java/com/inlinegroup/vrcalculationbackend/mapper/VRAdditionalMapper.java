package com.inlinegroup.vrcalculationbackend.mapper;

import com.inlinegroup.vrcalculationbackend.api.VRTypeCalculation;
import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VRAdditionalMapper {

    VRTypeCalculation toVRTypeCalculation(VRZifMainObject vrZifMainObject);
}
