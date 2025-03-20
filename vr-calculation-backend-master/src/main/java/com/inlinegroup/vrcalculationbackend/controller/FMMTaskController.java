package com.inlinegroup.vrcalculationbackend.controller;

import com.inlinegroup.vrcalculationbackend.aspect.ValidateParams;
import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;
import com.inlinegroup.vrcalculationbackend.service.FMMTaskService;
import com.inlinegroup.vrcalculationbackend.service.VRStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class FMMTaskController {
    public static final String OAUTH_SCHEME_NAME = "oAuth2";
    public static final String QUERY_PARAM_OBJECT_ID = "objectId";
    public static final String QUERY_PARAM_TIME_LEFT = "timeLeft";
    public static final String QUERY_PARAM_TIME_RIGHT = "timeRight";
    public static final String QUERY_PARAM_TIME = "time";
    private final FMMTaskService fmmTaskService;
    private final VRStorageService vrStorageService;

    public FMMTaskController(FMMTaskService fmmTaskService, VRStorageService vrStorageService) {
        this.fmmTaskService = fmmTaskService;
        this.vrStorageService = vrStorageService;
    }

    @Operation(summary = "Execute FMM Task",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/fmm")
    @ValidateParams
    public Flux<Void> executeFMMTask(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId,
            @RequestParam(value = QUERY_PARAM_TIME) String time) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        return vrZifMainObject.flatMapMany(mainObject ->
                fmmTaskService.executeFMMTask(objectId, time, mainObject.getName()));
    }

    @Operation(summary = "Execute FMM Task for a period of time",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/fmm/duration")
    @ValidateParams
    public Mono<Void> executeFMMTaskDuration(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId,
            @RequestParam(value = QUERY_PARAM_TIME_LEFT) String timeLeft,
            @RequestParam(value = QUERY_PARAM_TIME_RIGHT) String timeRight) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        return vrZifMainObject.flatMap(mainObject ->
                fmmTaskService.executeFMMTaskDuration(objectId, timeLeft, timeRight, mainObject.getName()));
    }

    @Operation(summary = "Execute FMM Task for a period of time (execute parallel)",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/fmm/duration/parallel")
    @ValidateParams
    public Mono<Void> executeFMMTaskDurationParallel(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId,
            @RequestParam(value = QUERY_PARAM_TIME_LEFT) String timeLeft,
            @RequestParam(value = QUERY_PARAM_TIME_RIGHT) String timeRight) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        return vrZifMainObject.flatMap(mainObject ->
                fmmTaskService.executeFMMTaskDurationParallel(objectId, timeLeft, timeRight, mainObject.getName()));
    }
}
