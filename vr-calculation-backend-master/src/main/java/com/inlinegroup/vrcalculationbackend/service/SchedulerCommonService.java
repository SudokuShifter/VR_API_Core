package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.api.fmm.FMMTaskDto;
import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;
import com.inlinegroup.vrcalculationbackend.service.enums.TaskStatus;
import com.inlinegroup.vrcalculationbackend.service.utils.TaskCurrentStatus;
import com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.DEFAULT_NUMBER_OF_RE_REQUEST_IN_WINDOW_SCHEDULER;
import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.TIME_STEP;
import static com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils.getTimeMinusDays;

@Service
@Slf4j
public class SchedulerCommonService {

    public static final String ER_MSG_TASK_EXECUTION_ERROR = "Task execution error";

    private final FMMTaskService fmmTaskService;
    private final MLTaskService mlTaskService;
    private final VRAdaptationAndValidationService vrAdaptationAndValidationService;
    private final VRStorageService vrStorageService;
    private final VRCalcConfig config;

    public SchedulerCommonService(FMMTaskService fmmTaskService, MLTaskService mlTaskService,
                                  VRAdaptationAndValidationService vrAdaptationAndValidationService,
                                  VRStorageService vrStorageService,
                                  VRCalcConfig config) {
        this.fmmTaskService = fmmTaskService;
        this.mlTaskService = mlTaskService;
        this.vrAdaptationAndValidationService = vrAdaptationAndValidationService;
        this.vrStorageService = vrStorageService;
        this.config = config;
    }

    public void executeFmmMlAndValidationTaskInParallel(LocalDateTime curTime) {
        List<VRZifMainObject> mainObjects = vrStorageService.getMainObjectList();
        if (!mainObjects.isEmpty()) {
            try {
                for (VRZifMainObject vrZifMainObject : mainObjects) {
                    vrAdaptationAndValidationService
                            .executeTaskValidation(
                                    vrZifMainObject.getZifUid(),
                                    getTimeMinusDays(TimeUtils.toString(curTime),
                                            config.getVrTaskValidationCountDays()),
                                    TimeUtils.toString(curTime))
                            .thenMany(executeFMMAndMLTask(vrZifMainObject.getZifUid(),
                                    TimeUtils.toString(curTime), vrZifMainObject.getName())).subscribe();
                }
            } catch (Exception ex) {
                log.error(ER_MSG_TASK_EXECUTION_ERROR + " - " + ex.getMessage());
            }
        }
    }

    /**
     * Метод выполнения FMM и ML задачи за указанную точку времени
     *
     * @param objectId      uid объекта модели (скважина)
     * @param date          метка времени UTC in ISO8601 format (2023-11-20T13:17:00.000Z)
     * @param logObjectName дополнительные сведения для операции логирования
     */
    public Flux<Void> executeFMMAndMLTask(String objectId, String date, String logObjectName) {
        Mono<FMMTaskDto> fmmTaskDtoMono = fmmTaskService.getRequestDataFMMTask(objectId, date);
        return fmmTaskService.executeFMMTask(fmmTaskDtoMono, objectId, date, logObjectName)
                .thenMany(mlTaskService.executeMLTask(objectId, date, logObjectName, fmmTaskDtoMono));
    }

    public void executeValidationTaskInOrder(LocalDateTime curTime) {
        List<VRZifMainObject> mainObjects = vrStorageService.getMainObjectList();
        if (!mainObjects.isEmpty()) {
            for (VRZifMainObject vrZifMainObject : mainObjects) {
                try {
                    vrAdaptationAndValidationService.executeTaskValidation(
                                    vrZifMainObject.getZifUid(),
                                    getTimeMinusDays(TimeUtils.toString(curTime),
                                            config.getVrTaskValidationCountDays()),
                                    TimeUtils.toString(curTime))
                            .then().block();
                } catch (Exception ex) {
                    log.error(ER_MSG_TASK_EXECUTION_ERROR + " - " + ex.getMessage());
                }
            }
        }
    }

    public void executeValidationTask(String uid, LocalDateTime curTime, LocalDateTime curTimeWithDeep) {
        try {
            vrAdaptationAndValidationService.executeTaskValidation(
                            uid,
                            getTimeMinusDays(TimeUtils.toString(curTime),
                                    config.getVrTaskValidationCountDays()),
                            TimeUtils.toString(curTimeWithDeep))
                    .then().block();
        } catch (Exception ex) {
            log.error(ER_MSG_TASK_EXECUTION_ERROR + " - " + ex.getMessage());
        }

    }

    public void executeValidationTaskInOrder(List<VRZifMainObject> mainObjects, TaskCurrentStatus status) {
        if (!mainObjects.isEmpty()) {
            for (VRZifMainObject vrZifMainObject : mainObjects) {
                try {
                    if (!status.isBusy(vrZifMainObject.getZifUid())) {
                        status.setStatus(vrZifMainObject.getZifUid(), true);
                        vrAdaptationAndValidationService.executeTaskValidation(
                                        vrZifMainObject.getZifUid(),
                                        getTimeMinusDays(TimeUtils.toString(mainObjects.getLast().getCurrentDateScheduler()),
                                                config.getVrTaskValidationCountDays()),
                                        TimeUtils.toString(mainObjects.getLast().getCurrentDateScheduler()))
                                .then().block();
                    }
                } catch (Exception ex) {
                    log.error(ER_MSG_TASK_EXECUTION_ERROR + " - " + ex.getMessage());
                } finally {
                    status.setStatus(vrZifMainObject.getZifUid(), false);
                }
            }
        }
    }

    @Async(value = "taskExecutor")
    public void taskExecutor(VRZifMainObject mainObject, TaskCurrentStatus taskCurStatus) {
        log.info("Start task executor for: " + mainObject.getName());
        VRZifMainObject curMainObject = mainObject;
        LocalDateTime curTime;
        try {
            taskCurStatus.setStatus(mainObject.getZifUid(), true);
            while (isFoundingGoodValue(
                    Objects.requireNonNull(curMainObject).getZifUid(),
                    curMainObject.getCurrentDateScheduler(),
                    curMainObject.getName(),
                    taskCurStatus) && isTaskOn(curMainObject.getZifUid())) {
                curTime = curMainObject.getCurrentDateScheduler();

                executeFMMAndMLTaskWithStatus(curMainObject, curTime, taskCurStatus);

                if (taskCurStatus.getErrorCount(curMainObject.getZifUid()) == 0) {
                    curTime = curTime.plusSeconds(TIME_STEP);
                } else if (taskCurStatus.getErrorCount(curMainObject.getZifUid()) >
                        DEFAULT_NUMBER_OF_RE_REQUEST_IN_WINDOW_SCHEDULER) {
                    curTime = curTime.plusSeconds(TIME_STEP);
                    taskCurStatus.resetErrorCounter(curMainObject.getZifUid());
                }
                curMainObject = saveCureTime(curMainObject.getZifUid(), curTime);
            }
        } catch (Exception ex) {
            log.error("Task executor get error. " + mainObject.getName() + " Msg: " + ex.getMessage());
        } finally {
            taskCurStatus.setStatus(mainObject.getZifUid(), false);
        }
    }

    private void executeFMMAndMLTaskWithStatus(VRZifMainObject mainObject, LocalDateTime time,
                                               TaskCurrentStatus taskCurStatus) {
        try {
            log.info("--- EXECUTE TASK: " + mainObject.getName() + " TIME: " + time + " ---");
            executeFMMAndMLTask(mainObject.getZifUid(), TimeUtils.toString(time), mainObject.getName())
                    .blockLast();
            taskCurStatus.resetErrorCounter(mainObject.getZifUid());
        } catch (Exception ex) {
            taskCurStatus.increaseErrorCounter(mainObject.getZifUid());
            log.error("Task executor get error. " + mainObject.getName() +
                   " Attempt: " + taskCurStatus.getErrorCount(mainObject.getZifUid()) + " Msg: " + ex.getMessage());
        }
    }

    private VRZifMainObject saveCureTime(String uid, LocalDateTime time) {
        VRZifMainObject curMainObject = vrStorageService.getObjectByUid(uid).block();
        if (curMainObject != null) {
            curMainObject.setCurrentDateScheduler(time);
            vrStorageService.saveMainObject(curMainObject).subscribe();
        }
        return curMainObject;
    }

    private boolean isFoundingGoodValue(String uid, LocalDateTime curTime, String logObjectName,
                                        TaskCurrentStatus taskCurStatus) {
        if (curTime.isBefore(taskCurStatus.getGoodValidDataBeforeTime(uid))) {
            return true;
        }
        LocalDateTime curTimeWithDeep =
                curTime.plusSeconds((long) config.getVrTaskSchedulerDeepValidation() * TIME_STEP);
        boolean isValid = vrAdaptationAndValidationService.isValidTagsNext(uid, curTimeWithDeep, logObjectName);
        if (isValid) {
            executeValidationTask(uid, curTime, curTimeWithDeep);
            taskCurStatus.setGoodValidDataBeforeTime(uid, curTimeWithDeep);
            return true;
        }
        return false;
    }

    private boolean isTaskOn(String uid) {
        VRZifMainObject mainObject = vrStorageService.getObjectByUid(uid).block();
        if (mainObject != null && mainObject.getSchedulerStatus() != null) {
            return mainObject.getSchedulerStatus().equals(TaskStatus.ON.toString());
        }
        return false;
    }
}
