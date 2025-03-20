package com.inlinegroup.vrcalculationbackend.service.storage;

import com.inlinegroup.vrcalculationbackend.api.TagDataTimeIntervalDto;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ValidationDataStorage {
    TimeInterval getTimeInterval(String objectId, String timeStart, String timeEnd)
            throws InMemoryValidationStorageException;

    Mono<TimeInterval> getTimeIntervalMono(String objectId, String timeStart, String timeEnd);

    List<TagDataTimeIntervalDto> getAndSaveTagsData(
            String objectId, String timeStart, String timeEnd, List<TagDataTimeIntervalDto> platformTagsData)
            throws InMemoryValidationStorageException;

    Mono<List<TagDataTimeIntervalDto>> getAndSaveTagsDataMono(
            String objectId, String timeStart, String timeEnd, List<TagDataTimeIntervalDto> platformTagsData);
}
