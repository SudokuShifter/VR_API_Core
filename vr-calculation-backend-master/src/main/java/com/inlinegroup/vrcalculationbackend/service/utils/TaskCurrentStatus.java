package com.inlinegroup.vrcalculationbackend.service.utils;

import com.inlinegroup.vrcalculationbackend.model.VRZifMainObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskCurrentStatus {
    private final Map<String, Boolean> taskCurStatus;
    private final Map<String, LocalDateTime> goodValidDataBeforeTime;
    private final Map<String, Integer> errorCounter;

    public TaskCurrentStatus(List<VRZifMainObject> mainObjects, LocalDateTime defaultTaskDateStart) {
        taskCurStatus = new ConcurrentHashMap<>();
        goodValidDataBeforeTime = new ConcurrentHashMap<>();
        errorCounter = new ConcurrentHashMap<>();
        for (VRZifMainObject mainObject : mainObjects) {
            errorCounter.put(mainObject.getZifUid(), 0);
            taskCurStatus.put(mainObject.getZifUid(), false);
            goodValidDataBeforeTime.put(mainObject.getZifUid(), defaultTaskDateStart);
        }
    }

    public boolean isBusy(String uid) {
        if (taskCurStatus.get(uid) == null) {
            return false;
        }
        return taskCurStatus.get(uid);
    }

    public void setStatus(String uid, boolean value) {
        taskCurStatus.put(uid, value);
    }

    public void setGoodValidDataBeforeTime(String uid, LocalDateTime date) {
        goodValidDataBeforeTime.put(uid, date);
    }

    public LocalDateTime getGoodValidDataBeforeTime(String uid) {
        return goodValidDataBeforeTime.get(uid);
    }

    public Integer getErrorCount(String uid){
        return errorCounter.get(uid);
    }

    public void resetErrorCounter(String uid){
        errorCounter.put(uid, 0);
    }

    public void increaseErrorCounter(String uid){
        errorCounter.put(uid, errorCounter.get(uid) + 1);
    }
}
