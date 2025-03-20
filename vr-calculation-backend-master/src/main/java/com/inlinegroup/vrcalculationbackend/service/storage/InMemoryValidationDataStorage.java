package com.inlinegroup.vrcalculationbackend.service.storage;

import com.inlinegroup.vrcalculationbackend.api.TagDataTimeIntervalDto;
import com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.TIME_STEP;

@Slf4j
public class InMemoryValidationDataStorage implements ValidationDataStorage {
    private final Map<String, List<TagDataTimeIntervalDto>> tagDataStorage;

    public InMemoryValidationDataStorage() {
        this.tagDataStorage = new ConcurrentHashMap<>();
    }

    public InMemoryValidationDataStorage(Map<String, List<TagDataTimeIntervalDto>> tagDataStorage) {
        this.tagDataStorage = tagDataStorage;
    }

    public Mono<TimeInterval> getTimeIntervalMono(String objectId, String timeStart, String timeEnd) {
        TimeInterval timeInterval;
        try {
            timeInterval = getTimeInterval(objectId, timeStart, timeEnd);
        } catch (InMemoryValidationStorageException ex) {
            return Mono.error(new InMemoryValidationStorageException(ex.getMessage()));
        }
        return Mono.just(timeInterval);
    }

    public Mono<List<TagDataTimeIntervalDto>> getAndSaveTagsDataMono(
            String objectId, String timeStart, String timeEnd, List<TagDataTimeIntervalDto> platformTagsData) {
        List<TagDataTimeIntervalDto> tagsData;
        try {
            tagsData = getAndSaveTagsData(objectId, timeStart, timeEnd, platformTagsData);
        } catch (InMemoryValidationStorageException ex) {
            return Mono.error(new InMemoryValidationStorageException(ex.getMessage()));
        }
        return Mono.just(tagsData);
    }

    /**
     * Метод получения временных интервалов для запроса данных из платформы. Внутри объекта TimeInterval
     * устанавливается флаг необходимости запроса.
     * После получения объекта TimeInterval необходимо произвести запрос данных из платформы и выполнить
     * получение полного списка данных через метод getAndSaveTagsData.
     *
     * @param objectId  uid объекта модели (скважина)
     * @param timeStart метка времени начала выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param timeEnd   метка времени окончания выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @return TimeInterval
     */
    public TimeInterval getTimeInterval(String objectId, String timeStart, String timeEnd)
            throws InMemoryValidationStorageException {
        if (!tagDataStorage.containsKey(objectId)) {
            return TimeInterval.builder()
                    .isRequest(true)
                    .timeStart(TimeUtils.getFrom(timeStart))
                    .timeEnd(TimeUtils.getFrom(timeEnd))
                    .build();
        }
        List<TagDataTimeIntervalDto> tagData = getTagDataList(objectId);
        LocalDateTime timeStartStorage = tagData.get(0).getTimeStart();
        LocalDateTime timeEndStorage = tagData.get(0).getTimeEnd();
        return calcTimeInterval(TimeUtils.getFrom(timeStart), TimeUtils.getFrom(timeEnd),
                timeStartStorage, timeEndStorage);
    }

    /**
     * Метод формирования полного списка данных. Выполняется после метода getTimeInterval.
     *
     * @param objectId         uid объекта модели (скважина)
     * @param timeStart        метка времени начала выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param timeEnd          метка времени окончания выборки данных. Формат UTC in ISO8601 format (2023-11-20T13:17:20.000Z)
     * @param platformTagsData запрошенный из платформы список данных за время, рассчитанное в getTimeInterval
     * @return List<TagDataTimeIntervalDto>
     */
    public List<TagDataTimeIntervalDto> getAndSaveTagsData(
            String objectId, String timeStart, String timeEnd, List<TagDataTimeIntervalDto> platformTagsData)
            throws InMemoryValidationStorageException {
        if (!tagDataStorage.containsKey(objectId)) {
            if (!platformTagsData.isEmpty() &&
                    platformTagsData.get(0).getTimeStart().equals(TimeUtils.getFrom(timeStart)) &&
                    platformTagsData.get(0).getTimeEnd().equals(TimeUtils.getFrom(timeEnd))) {
                tagDataStorage.put(objectId, platformTagsData);
                log.debug("Tags added in memory validation data base. ObjectId: " + objectId);
                return platformTagsData;
            }
            throw new InMemoryValidationStorageException("Transmitted truncated tag value. Full tag value is missing " +
                    "in memory database.");
        }
        List<TagDataTimeIntervalDto> storageTagData = getTagDataList(objectId);
        LocalDateTime timeStartStorage = storageTagData.get(0).getTimeStart();
        LocalDateTime timeEndStorage = storageTagData.get(0).getTimeEnd();
        LocalDateTime timeStartCur = TimeUtils.getFrom(timeStart);
        LocalDateTime timeEndCur = TimeUtils.getFrom(timeEnd);
        if (timeStartCur.equals(timeStartStorage) && timeEndCur.equals(timeEndStorage)) {
            return tagDataStorage.get(objectId);
        }
        if (!platformTagsData.isEmpty() && timeStartCur.equals(platformTagsData.get(0).getTimeStart()) &&
                timeEndCur.equals(platformTagsData.get(0).getTimeEnd())) {
            tagDataStorage.put(objectId, platformTagsData);
            return platformTagsData;
        }
        if (!platformTagsData.isEmpty() && timeStartCur.equals(platformTagsData.get(0).getTimeStart())) {
            return addLeftPartStorage(platformTagsData, storageTagData);
        }
        if (!platformTagsData.isEmpty() && timeEndCur.equals(platformTagsData.get(0).getTimeEnd())) {
            return addRightPartStorage(platformTagsData, storageTagData);
        }
        throw new InMemoryValidationStorageException("The start (end) time doesn't correspond to the value of the " +
                "start (end) of the time array");
    }

    private List<TagDataTimeIntervalDto> addRightPartStorage(List<TagDataTimeIntervalDto> platformTagsData,
                                                             List<TagDataTimeIntervalDto> storageTagData)
            throws InMemoryValidationStorageException {
        for (int i = 0; i < platformTagsData.size(); ++i) {
            if (!platformTagsData.get(i).getName().equals(storageTagData.get(i).getName())) {
                throw new InMemoryValidationStorageException("The order of the tags in the list does not match.");
            }
            for (int j = 0; j < platformTagsData.get(i).getValues().size(); ++j) {
                storageTagData.get(i).getValues().removeFirst();
                storageTagData.get(i).getValues().add(platformTagsData.get(i).getValues().get(j));
                storageTagData.get(i).getTimestamps().removeFirst();
                storageTagData.get(i).getTimestamps().add(platformTagsData.get(i).getTimestamps().get(j));
            }
            storageTagData.get(i).setTimeStart(TimeUtils.getFrom(storageTagData.get(i).getTimestamps().getFirst()));
            storageTagData.get(i).setTimeEnd(TimeUtils.getFrom(storageTagData.get(i).getTimestamps().getLast()));
        }
        return storageTagData;
    }

    private List<TagDataTimeIntervalDto> addLeftPartStorage(List<TagDataTimeIntervalDto> platformTagsData,
                                                            List<TagDataTimeIntervalDto> storageTagData)
            throws InMemoryValidationStorageException {
        for (int i = 0; i < platformTagsData.size(); ++i) {
            if (!platformTagsData.get(i).getName().equals(storageTagData.get(i).getName())) {
                throw new InMemoryValidationStorageException("The order of the tags in the list does not match.");
            }
            LinkedList<Double> reversedValues = platformTagsData.get(i).getValues().reversed();
            LinkedList<String> reversedTimestamps = platformTagsData.get(i).getTimestamps().reversed();
            platformTagsData.get(i).getTimestamps().reversed();
            for (int j = 0; j < platformTagsData.get(i).getValues().size(); ++j) {
                storageTagData.get(i).getValues().removeLast();
                storageTagData.get(i).getValues().addFirst(reversedValues.get(j));
                storageTagData.get(i).getTimestamps().removeLast();
                storageTagData.get(i).getTimestamps().addFirst(reversedTimestamps.get(j));
            }
            storageTagData.get(i).setTimeStart(TimeUtils.getFrom(storageTagData.get(i).getTimestamps().getFirst()));
            storageTagData.get(i).setTimeEnd(TimeUtils.getFrom(storageTagData.get(i).getTimestamps().getLast()));
        }
        return storageTagData;
    }

    private List<TagDataTimeIntervalDto> getTagDataList(String objectId) throws InMemoryValidationStorageException {
        List<TagDataTimeIntervalDto> tagData = tagDataStorage.get(objectId);
        if (tagData == null || tagData.isEmpty() || tagData.get(0).getTimestamps() == null ||
                tagData.get(0).getTimestamps().isEmpty()) {
            throw new InMemoryValidationStorageException("Tag data not found. Object: " + objectId);
        }
        return tagData;
    }

    private TimeInterval calcTimeInterval(LocalDateTime timeStart, LocalDateTime timeEnd,
                                          LocalDateTime timeStartStorage, LocalDateTime timeEndStorage) {
        if (timeStart.equals(timeStartStorage) && timeEnd.equals(timeEndStorage)) {
            return TimeInterval.builder().isRequest(false).build();
        }
        if (timeStartStorage.isBefore(timeEnd) && (timeEndStorage.isAfter(timeEnd) || timeEndStorage.equals(timeEnd)) &&
                timeStartStorage.isAfter(timeStart)) {
            return TimeInterval.builder()
                    .isRequest(true)
                    .timeStart(timeStart)
                    .timeEnd(timeStartStorage.minusSeconds(TIME_STEP))
                    .build();
        }
        if ((timeStartStorage.isBefore(timeStart) || timeStartStorage.equals(timeStart))
                && timeEndStorage.isAfter(timeStart) && timeEndStorage.isBefore(timeEnd)) {
            return TimeInterval.builder()
                    .isRequest(true)
                    .timeStart(timeEndStorage.plusSeconds(TIME_STEP))
                    .timeEnd(timeEnd)
                    .build();
        }
        return TimeInterval.builder()
                .isRequest(true)
                .timeStart(timeStart)
                .timeEnd(timeEnd)
                .build();
    }
}