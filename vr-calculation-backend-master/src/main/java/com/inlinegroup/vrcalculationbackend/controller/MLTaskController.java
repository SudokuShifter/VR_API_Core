package com.inlinegroup.vrcalculationbackend.controller;

import com.inlinegroup.vrcalculationbackend.aspect.ValidateParams;
import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;
import com.inlinegroup.vrcalculationbackend.service.MLTaskService;
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
public class MLTaskController {
    public static final String OAUTH_SCHEME_NAME = "oAuth2";
    public static final String QUERY_PARAM_OBJECT_ID = "objectId";
    public static final String QUERY_PARAM_TIME_LEFT = "timeLeft";
    public static final String QUERY_PARAM_TIME_RIGHT = "timeRight";
    public static final String QUERY_PARAM_TIME = "time";
    private final MLTaskService mlTaskService;
    private final VRStorageService vrStorageService;

    public MLTaskController(MLTaskService mlTaskService,
                            VRStorageService vrStorageService) {
        this.mlTaskService = mlTaskService;
        this.vrStorageService = vrStorageService;
    }

    @Operation(summary = "Execute ML Task",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/ml")
    @ValidateParams
    public Flux<Void> executeMLTask(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId,
            @RequestParam(value = QUERY_PARAM_TIME) String time) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        return vrZifMainObject.flatMapMany(mainObject ->
                mlTaskService.executeMLTask(objectId, time, mainObject.getName()));
    }

    @Operation(summary = "Execute ML Task for a period of time",
            security = {@SecurityRequirement(name = OAUTH_SCHEME_NAME)})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful request"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Data not found",
                    content = @Content)})
    @GetMapping("/ml/duration")
    @ValidateParams
    public Mono<Void> executeMLTaskDuration(
            @RequestParam(value = QUERY_PARAM_OBJECT_ID) String objectId,
            @RequestParam(value = QUERY_PARAM_TIME_LEFT) String timeLeft,
            @RequestParam(value = QUERY_PARAM_TIME_RIGHT) String timeRight) {
        Mono<VRZifMainObject> vrZifMainObject = vrStorageService.getObjectByUid(objectId);
        return vrZifMainObject.flatMap(mainObject ->
                mlTaskService.executeMLTaskDuration(objectId, timeLeft, timeRight, mainObject.getName()));
    }
}
