package com.inlinegroup.vrcalculationbackend.controller;

import com.inlinegroup.vrcalculationbackend.api.VRTypeCalculation;
import com.inlinegroup.vrcalculationbackend.aspect.ValidateParams;
import com.inlinegroup.vrcalculationbackend.service.AdditionalFunctionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class AdditionalFunctionalController {

    public static final String TYPE_VALUE = "typeValue";
    public static final String OAUTH_SCHEME_NAME = "oAuth2";

    private final AdditionalFunctionalService additionalFunctionalService;

    public AdditionalFunctionalController(AdditionalFunctionalService additionalFunctionalService) {
        this.additionalFunctionalService = additionalFunctionalService;
    }

    @Operation(summary = "Get all type calculation",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/type-calculation/all")
    public Flux<VRTypeCalculation> getAllAdaptData() {
        return additionalFunctionalService.getAllTypeCalculation();
    }

    @Operation(summary = "Get type calculation by object uid",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/type-calculation/{objectUid}")
    @ValidateParams
    public Mono<VRTypeCalculation> getValidateData(@PathVariable String objectUid) {
        return additionalFunctionalService.getTypeCalculationById(objectUid);
    }

    @Operation(summary = "Set active type calculation by object uid",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @PutMapping("/type-calculation/set/{objectUid}")
    @ValidateParams
    public Mono<Void> setActiveAdaptData(
            @PathVariable String objectUid,
            @RequestParam(value = TYPE_VALUE) String typeValue) {
        return additionalFunctionalService.setTypeCalculationValueById(objectUid, typeValue);
    }
}
