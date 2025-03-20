package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.config.TypeScheduler;
import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;
import com.inlinegroup.vrcalculationbackend.service.enums.TaskStatus;
import com.inlinegroup.vrcalculationbackend.service.utils.TaskCurrentStatus;
import com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SchedulerDataFoundService {

    private final VRCalcConfig config;
    private final VRStorageService vrStorageService;
    private final SchedulerCommonService schedulerCommonService;
    private Boolean isInit;
    private final TaskCurrentStatus taskCurStatus;

    public SchedulerDataFoundService(VRCalcConfig config,
                                     VRStorageService vrStorageService,
                                     SchedulerCommonService schedulerCommonService) {
        this.config = config;
        this.vrStorageService = vrStorageService;
        this.schedulerCommonService = schedulerCommonService;
        isInit = false;
        taskCurStatus = new TaskCurrentStatus(
                vrStorageService.getMainObjectList(),
                TimeUtils.getFrom(config.getVrTaskDateStart()));
    }

    @Scheduled(cron = "#{@getCronValue}")
    public void taskScheduler() {
        if (config.getTypeScheduler() != TypeScheduler.DATA_FOUND) {
            return;
        }
        List<VRZifMainObject> mainObjects = getMainObjects();
        if (Boolean.FALSE.equals(isInit)) {
            log.info("------------------------ INIT DATA FOUND SCHEDULER ------------------------------");
            schedulerCommonService.executeValidationTaskInOrder(mainObjects, taskCurStatus);
            isInit = true;
            return;
        }
        log.info("---------------------- SCHEDULER DATA FOUND ---------------------------");
        for (VRZifMainObject mainObject : mainObjects) {
            if (!taskCurStatus.isBusy(mainObject.getZifUid())) {
                schedulerCommonService.taskExecutor(mainObject, taskCurStatus);
            }
        }
    }

    /**
     * Получаем все объекты скважин из БД и если текущая дата отсутствует в базе данных, устанавливаем дату начала
     * по умолчанию. Если скважина в статусе Off, она не включается в список.
     *
     * @return List<VRZifMainObject>
     */
    private List<VRZifMainObject> getMainObjects() {
        List<VRZifMainObject> mainObjects = vrStorageService.getMainObjectList();
        List<VRZifMainObject> resMainObjects = new ArrayList<>();
        for (VRZifMainObject mainObject : mainObjects) {
            if (mainObject.getCurrentDateScheduler() == null) {
                mainObject.setCurrentDateScheduler(TimeUtils.getFrom(
                        TimeUtils.correctEndDate(config.getVrTaskDateStart())));
            }
            if (mainObject.getSchedulerStatus() == null ||
                    mainObject.getSchedulerStatus().contains(TaskStatus.OFF.toString())) {
                continue;
            }
            resMainObjects.add(mainObject);
        }
        return resMainObjects;
    }
}
