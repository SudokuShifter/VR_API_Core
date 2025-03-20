package com.inlinegroup.vrcalculationbackend.controller;

import com.inlinegroup.vrcalculationbackend.api.VRAdaptationDataResponse;
import com.inlinegroup.vrcalculationbackend.api.VRValidationDataResponse;
import com.inlinegroup.vrcalculationbackend.api.VRValidationRecordResponse;
import com.inlinegroup.vrcalculationbackend.aspect.ValidateParams;
import com.inlinegroup.vrcalculationbackend.service.VRAdaptationAndValidationService;
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
public class AdaptationAndValidationController {

    public static final String OAUTH_SCHEME_NAME = "oAuth2";
    public static final String QUERY_PARAM_OBJECT_ID = "objectId";
    public static final String QUERY_PARAM_TIME_LEFT = "timeLeft";
    public static final String QUERY_PARAM_TIME_RIGHT = "timeRight";
    public static final String QUERY_PARAM_NAME = "adaptName";
    public static final String QUERY_IS_USER_VALUE = "isUserValue";
    public static final String QUERY_WCT = "wct";
    public static final String QUERY_GAS_CONDENSATE_FACTOR = "gasCondensateFactor";

    private final VRAdaptationAndValidationService vrAdaptationAndValidationService;

    public AdaptationAndValidationController(VRAdaptationAndValidationService vrAdaptationAndValidationService) {
        this.vrAdaptationAndValidationService = vrAdaptationAndValidationService;
    }

    @Operation(summary = "Calculate validate data",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/validate")
    @ValidateParams
    public Mono<VRValidationDataResponse> getTaskValidateValue(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId,
            @RequestParam(value = QUERY_PARAM_TIME_LEFT) String timeLeft,
            @RequestParam(value = QUERY_PARAM_TIME_RIGHT) String timeRight) {
        return vrAdaptationAndValidationService.executeTaskValidation(objectId, timeLeft, timeRight);
    }

    @Operation(summary = "Get validate data from DB",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/validate/get")
    @ValidateParams
    public Mono<VRValidationRecordResponse> getValidateData(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId) {
        return vrAdaptationAndValidationService.getValidationDataFromDB(objectId);
    }

    @Operation(summary = "Set validate data",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @PutMapping("/validate/set")
    @ValidateParams
    public Mono<Void> getValidateData(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId,
            @RequestParam(value = QUERY_IS_USER_VALUE) Boolean isUserValue,
            @RequestParam(value = QUERY_WCT) Double wct,
            @RequestParam(value = QUERY_GAS_CONDENSATE_FACTOR) Double gasCondensateFactor) {
        return vrAdaptationAndValidationService.setValidationData(objectId, isUserValue, wct, gasCondensateFactor);
    }

    @Operation(summary = "Calculate adaptation coefficients without activating it",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/adaptation")
    @ValidateParams
    public Mono<VRAdaptationDataResponse> getTaskAdaptationValue(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId,
            @RequestParam(value = QUERY_PARAM_TIME_LEFT) String timeLeft,
            @RequestParam(value = QUERY_PARAM_TIME_RIGHT) String timeRight,
            @RequestParam(value = QUERY_PARAM_NAME) String name) {
        return vrAdaptationAndValidationService.executeTaskAdaptationAndSave(objectId, timeLeft, timeRight, name);
    }

    @Operation(summary = "Get all the adaptation data",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/adaptation/all")
    @ValidateParams
    public Flux<VRAdaptationDataResponse> getAllAdaptData(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId) {
        return vrAdaptationAndValidationService.getAllAdaptationDataByObjectId(objectId);
    }

    @Operation(summary = "Get active adaptation data",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/adaptation/active")
    @ValidateParams
    public Mono<VRAdaptationDataResponse> getActiveAdaptData(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId) {
        return vrAdaptationAndValidationService.getActiveAdaptationDataByObjectId(objectId);
    }

    @Operation(summary = "Set active adaptation data",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @PutMapping("/adaptation/set")
    @ValidateParams
    public Mono<Void> setActiveAdaptData(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId,
            @RequestParam(value = QUERY_PARAM_NAME) String name) {
        return vrAdaptationAndValidationService.setActiveAdaptationDataByName(objectId, name);
    }
}
