package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.zif.uom.UomDto;
import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import com.inlinegroup.vrcalculationbackend.service.enums.AppType;
import com.inlinegroup.vrcalculationbackend.service.utils.ZifUri;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ZifUomService {

    private final VRCalcConfig config;
    private final WebClientExecutionService webClientExecutionService;

    public ZifUomService(VRCalcConfig config, WebClientExecutionService webClientExecutionService) {
        this.config = config;
        this.webClientExecutionService = webClientExecutionService;
    }

    /**
     * Метод получения единицы измерения по Id.
     * В случае получения статусов 404 будет выброшено исключение VRNotFoundClientException.
     * В случае получения статусов 4xx и 5xx будет выброшено исключение VRClientException.
     *
     * @param uomId             id единицы измерения
     * @param logAdditionalData дополнительные сведения для операции логирования
     * @return UomDto
     */
    public Mono<UomDto> getUomById(String uomId, String logAdditionalData) {
        ZifUri uri = new ZifUri.ZifUriBuilder(config)
                .setPath(config.getUriOmUomGetUomById())
                .addPathId(uomId)
                .build();
        return webClientExecutionService.
                executeMonoGetRequest(uri.getUri(), AppType.ZIF, new ParameterizedTypeReference<>() {
                }, "Get the conversion factor. " + logAdditionalData);
    }
}
