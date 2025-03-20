package com.inlinegroup.vrcalculationbackend.mapper;

import com.inlinegroup.vrcalculationbackend.api.fmm.FMMTaskDto;
import com.inlinegroup.vrcalculationbackend.api.ml.FeaturesDto;
import com.inlinegroup.vrcalculationbackend.api.ml.PredictRequestDto;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Mono;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.FACTOR_TEMPERATURE_KELVIN;

@Mapper(componentModel = "spring")
public interface MLMapper {
    MLMapper INSTANCE = Mappers.getMapper(MLMapper.class);

    FeaturesDto toMLTaskRequestDto(@MappingTarget FeaturesDto featuresDto, FMMTaskDto fmmTaskDto);

    default Mono<PredictRequestDto> toMLTaskRequestDtoMono(PredictRequestDto predictRequest,
                                                           FMMTaskDto fmmTaskDto) {
        return Mono.just(toMLTaskRequestDto(predictRequest, fmmTaskDto));
    }

    default PredictRequestDto toMLTaskRequestDto(PredictRequestDto predictRequest,
                                                 FMMTaskDto fmmTaskDto) {
        predictRequest.setFeatures(toMLTaskRequestDto(predictRequest.getFeatures(), fmmTaskDto));
        return predictRequest;
    }

    /**
     * Переводим в градусы только поля, которые берутся из FMM модели
     */
    @AfterMapping
    default void changeTemperature(@MappingTarget FeaturesDto featuresDto) {
        featuresDto.setPBuf(featuresDto.getTBuf() - FACTOR_TEMPERATURE_KELVIN);
        featuresDto.setTOut(featuresDto.getTOut() - FACTOR_TEMPERATURE_KELVIN);
    }
}