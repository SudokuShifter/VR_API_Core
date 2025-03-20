package com.inlinegroup.vrcalculationbackend.mapper;

import com.inlinegroup.vrcalculationbackend.api.ConstDataDto;
import com.inlinegroup.vrcalculationbackend.api.VRAdaptationDataResponse;
import com.inlinegroup.vrcalculationbackend.api.VRValidationDataResponse;
import com.inlinegroup.vrcalculationbackend.api.VRValidationRecordResponse;
import com.inlinegroup.vrcalculationbackend.api.fmm.AdaptTaskDto;
import com.inlinegroup.vrcalculationbackend.api.fmm.ValidationSolutionDto;
import com.inlinegroup.vrcalculationbackend.model.VRAdaptationData;
import com.inlinegroup.vrcalculationbackend.model.VRValidationData;
import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface VAMapper {
    FMMMapper INSTANCE = Mappers.getMapper(FMMMapper.class);

    @Mapping(source = "uid", target = "zifObjectUid")
    VRValidationRecordResponse toValidationResponse(VRValidationData vrValidationData, String uid);

    @Mapping(source = "gammaGs", target = "gammaGc")
    AdaptTaskDto toAdaptTaskDtoConst(@MappingTarget AdaptTaskDto adaptTaskDto, ConstDataDto constData);

    @Mapping(source = "vrZifMainObject.name", target = "zifObjectName")
    @Mapping(source = "vrZifMainObject.zifUid", target = "zifObjectUid")
    @Mapping(source = "vrAdaptationData.name", target = "adaptName")
    @Mapping(source = "vrAdaptationData.chokeAdaptValue", target = "cChokeAdapt")
    @Mapping(source = "vrAdaptationData.chokeAdaptPercent", target = "dChokePercentAdapt")
    @Mapping(source = "vrAdaptationData.creationDate", target = "creationDate")
    @Mapping(source = "vrAdaptationData.dateStart", target = "dateStart")
    @Mapping(source = "vrAdaptationData.dateEnd", target = "dateEnd")
    VRAdaptationDataResponse toAdaptationResponse(VRAdaptationData vrAdaptationData,
                                                  VRZifMainObject vrZifMainObject);

    @Mapping(source = "validationSolutionDto.wct", target = "wct")
    @Mapping(source = "validationSolutionDto.gasCondensateFactor", target = "gasCondensateFactor")
    @Mapping(source = "vrZifMainObject.name", target = "zifObjectName")
    @Mapping(source = "vrZifMainObject.zifUid", target = "zifObjectUid")
    VRValidationDataResponse toVrValidationDataResponse(ValidationSolutionDto validationSolutionDto,
                                                        VRZifMainObject vrZifMainObject);
}