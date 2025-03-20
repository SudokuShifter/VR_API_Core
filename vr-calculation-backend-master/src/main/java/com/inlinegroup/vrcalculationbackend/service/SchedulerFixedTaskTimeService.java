package com.inlinegroup.vrcalculationbackend.service;

import com.inlinegroup.vrcalculationbackend.config.TypeScheduler;
import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.TIME_STEP;
import static com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils.correctEndDate;

@Service
@Slf4j
public class SchedulerFixedTaskTimeService {
    private final SchedulerCommonService schedulerCommonService;
    private final VRCalcConfig config;
    private final LocalDateTime startTime;
    private LocalDateTime curTime;
    private Boolean isInit;

    public SchedulerFixedTaskTimeService(SchedulerCommonService schedulerCommonService,
                                         VRCalcConfig config) {
        this.schedulerCommonService = schedulerCommonService;
        this.config = config;
        isInit = false;
        startTime = TimeUtils.getFrom(config.getVrTaskDateStart());
        curTime = correctEndDate(startTime);
    }

    @Scheduled(cron = "#{@getCronValue}")
    public void executeFMMAndValidationBySchedule() {
        if (config.getTypeScheduler() != TypeScheduler.FIXED_TIME) {
            return;
        }
        if (curTime.isBefore(LocalDateTime.now().minusSeconds(TIME_STEP))) {
            if (startTime.equals(curTime)) {
                if (Boolean.FALSE.equals(isInit)) {
                    log.info("------------------------ INIT FIXED TIME SCHEDULER ------------------------------");
                    schedulerCommonService.executeValidationTaskInOrder(curTime);
                    curTime = curTime.plusSeconds(TIME_STEP);
                    isInit = true;
                }
            } else {
                log.info("---------------------- SCHEDULER FIXED TIME ---------------------------");
                schedulerCommonService.executeFmmMlAndValidationTaskInParallel(curTime);
                curTime = curTime.plusSeconds(TIME_STEP);
                curTime = correctEndDate(curTime);
            }
        }
    }
}
