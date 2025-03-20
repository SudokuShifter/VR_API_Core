package com.inlinegroup.vrcalculationbackend.mapper;

import com.inlinegroup.vrcalculationbackend.api.ConstDataDto;
import com.inlinegroup.vrcalculationbackend.api.fmm.FMMTaskDto;
import com.inlinegroup.vrcalculationbackend.model.VRAdaptationData;
import com.inlinegroup.vrcalculationbackend.model.VRValidationData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import static org.mapstruct.CollectionMappingStrategy.*;

@Mapper(componentModel = "spring", collectionMappingStrategy = ACCESSOR_ONLY)
public interface FMMMapper {
    FMMMapper INSTANCE = Mappers.getMapper(FMMMapper.class);

    @Mapping(source = "gammaGs", target = "gammaGc")
    FMMTaskDto toFMMTaskDtoConst(@MappingTarget FMMTaskDto fmmTaskDto, ConstDataDto constData);

    FMMTaskDto toFMMTaskDtoValidation(@MappingTarget FMMTaskDto fmmTaskDto, VRValidationData validationData);

    @Mapping(source = "chokeAdaptPercent", target = "DChokePercentAdapt")
    @Mapping(source = "chokeAdaptValue", target = "CChokeAdapt")
    FMMTaskDto toFMMTaskDtoAdaptation(@MappingTarget FMMTaskDto fmmTaskDto, VRAdaptationData vrAdaptationData);
}
