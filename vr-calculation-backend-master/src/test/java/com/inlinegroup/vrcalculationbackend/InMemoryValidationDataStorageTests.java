package com.inlinegroup.vrcalculationbackend;

import com.inlinegroup.vrcalculationbackend.api.TagDataTimeIntervalDto;
import com.inlinegroup.vrcalculationbackend.service.storage.InMemoryValidationDataStorage;
import com.inlinegroup.vrcalculationbackend.service.storage.InMemoryValidationStorageException;
import com.inlinegroup.vrcalculationbackend.service.storage.TimeInterval;
import com.inlinegroup.vrcalculationbackend.service.utils.TimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.TIME_STEP;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryValidationDataStorageTests {

    private static final String OBJECT_ID_1 = "Test_1";
    private static final String TAG_NAME_1 = "Tag name 1";
    private static final String TAG_NAME_2 = "Tag name 2";
    private static final String TAG_ID_1 = "Test tag 1";
    private static final String TAG_ID_2 = "Test tag 2";
    private static final Double[] TEST_VALUE_1 = {0.11, 0.12, 0.13, 0.14, 0.15};
    private static final Double[] TEST_VALUE_2 = {0.13, 0.14, 0.15, 0.16, 0.17};
    private static final Double[] TEST_VALUE_3 = {0.09, 0.10, 0.11, 0.12, 0.13};
    private static final Double[] TEST_VALUE_4 = {0.55, 0.56, 0.57, 0.58, 0.59};
    private static final Double[] TEST_VALUE_DATA_RIGHT_FROM_ONE = {0.16, 0.17};
    private static final Double[] TEST_VALUE_DATA_LEFT_FROM_ONE = {0.09, 0.10};
    private static final Double[] TEST_VALUE_DATA_RIGHT_RESULT = {0.13, 0.14, 0.15, 0.16, 0.17};
    private static final Double[] TEST_VALUE_DATA_LEFT_RESULT = {0.09, 0.10, 0.11, 0.12, 0.13};

    private static final String[] TEST_TIMESTAMP_1 = {"2023-11-20T15:30:00.000Z", "2023-11-20T15:30:30.000Z",
            "2023-11-20T15:31:00.000Z", "2023-11-20T15:31:30.000Z", "2023-11-20T15:32:00.000Z"};
    private static final String[] TEST_TIMESTAMP_2 = {"2023-11-20T15:31:00.000Z", "2023-11-20T15:31:30.000Z",
            "2023-11-20T15:32:00.000Z", "2023-11-20T15:32:30.000Z", "2023-11-20T15:33:00.000Z"};
    private static final String[] TEST_TIMESTAMP_3 = {"2023-11-20T15:29:00.000Z", "2023-11-20T15:29:30.000Z",
            "2023-11-20T15:30:00.000Z", "2023-11-20T15:30:30.000Z", "2023-11-20T15:31:00.000Z"};
    private static final String[] TEST_TIMESTAMP_4 = {"2023-11-20T16:29:00.000Z", "2023-11-20T16:29:30.000Z",
            "2023-11-20T16:30:00.000Z", "2023-11-20T16:30:30.000Z", "2023-11-20T16:31:00.000Z"};
    private static final String[] TEST_TIMESTAMP_RIGHT_FROM_ONE = {"2023-11-20T15:32:30.000Z",
            "2023-11-20T15:33:00.000Z"};
    private static final String[] TEST_TIMESTAMP_LEFT_FROM_ONE = {"2023-11-20T15:29:00.000Z",
            "2023-11-20T15:29:30.000Z"};
    private static final String[] TEST_TIMESTAMP_RIGHT_RESULT = {"2023-11-20T15:31:00.000Z", "2023-11-20T15:31:30.000Z",
            "2023-11-20T15:32:00.000Z", "2023-11-20T15:32:30.000Z", "2023-11-20T15:33:00.000Z"};
    private static final String[] TEST_TIMESTAMP_LEFT_RESULT = {"2023-11-20T15:29:00.000Z", "2023-11-20T15:29:30.000Z",
            "2023-11-20T15:30:00.000Z", "2023-11-20T15:30:30.000Z", "2023-11-20T15:31:00.000Z"};

    private InMemoryValidationDataStorage dataStorage;

    @BeforeEach
    void init() {
        List<TagDataTimeIntervalDto> tagList = new ArrayList<>();
        tagList.add(createTagDataList(TEST_VALUE_1, TEST_TIMESTAMP_1, TAG_NAME_1, TAG_ID_1));
        tagList.add(createTagDataList(TEST_VALUE_1, TEST_TIMESTAMP_1, TAG_NAME_2, TAG_ID_2));
        Map<String, List<TagDataTimeIntervalDto>> testData = new HashMap<>();
        testData.put(OBJECT_ID_1, tagList);
        dataStorage = new InMemoryValidationDataStorage(testData);
    }

    TagDataTimeIntervalDto createTagDataList(Double[] values, String[] timestamps,
                                             String tagName, String tagId) {
        return TagDataTimeIntervalDto.builder()
                .timeStart(TimeUtils.getFrom(timestamps[0]))
                .timeEnd(TimeUtils.getFrom(timestamps[timestamps.length - 1]))
                .id(tagId)
                .name(tagName)
                .values(new LinkedList<>(Arrays.asList(values)))
                .timestamps(new LinkedList<>(Arrays.asList(timestamps)))
                .build();
    }

    @Test
    void getTimeInterval_whenDataEquals_thenReturnFalse() throws InMemoryValidationStorageException {
        TimeInterval timeInterval =
                dataStorage.getTimeInterval(OBJECT_ID_1, TEST_TIMESTAMP_1[0], TEST_TIMESTAMP_1[4]);

        assertFalse(timeInterval.isRequest());
    }

    @Test
    void getTimeInterval_whenDataRight_thenReturnTimeInterval() throws InMemoryValidationStorageException {
        TimeInterval timeInterval =
                dataStorage.getTimeInterval(OBJECT_ID_1, TEST_TIMESTAMP_2[0], TEST_TIMESTAMP_2[4]);

        assertTrue(timeInterval.isRequest());
        assertEquals(timeInterval.timeStart(), TimeUtils.getFrom(TEST_TIMESTAMP_1[4]).plusSeconds(TIME_STEP));
        assertEquals(timeInterval.timeEnd(), TimeUtils.getFrom(TEST_TIMESTAMP_2[4]));
    }

    @Test
    void getTimeInterval_whenDataLeft_thenReturnTimeInterval() throws InMemoryValidationStorageException {
        TimeInterval timeInterval =
                dataStorage.getTimeInterval(OBJECT_ID_1, TEST_TIMESTAMP_3[0], TEST_TIMESTAMP_3[4]);

        assertTrue(timeInterval.isRequest());
        assertEquals(timeInterval.timeStart(), TimeUtils.getFrom(TEST_TIMESTAMP_3[0]));
        assertEquals(timeInterval.timeEnd(), TimeUtils.getFrom(TEST_TIMESTAMP_1[0]).minusSeconds(TIME_STEP));
    }

    @Test
    void getTimeInterval_whenArrayIsOutOfBounds_thenReturnTimeInterval() throws InMemoryValidationStorageException {
        TimeInterval timeInterval =
                dataStorage.getTimeInterval(OBJECT_ID_1, TEST_TIMESTAMP_4[0], TEST_TIMESTAMP_4[4]);

        assertTrue(timeInterval.isRequest());
        assertEquals(timeInterval.timeStart(), TimeUtils.getFrom(TEST_TIMESTAMP_4[0]));
        assertEquals(timeInterval.timeEnd(), TimeUtils.getFrom(TEST_TIMESTAMP_4[4]));
    }

    @Test
    void getTimeInterval_whenObjectNotFound_thenReturnTimeInterval() throws InMemoryValidationStorageException {
        TimeInterval timeInterval =
                dataStorage.getTimeInterval("object", TEST_TIMESTAMP_4[0], TEST_TIMESTAMP_4[4]);

        assertTrue(timeInterval.isRequest());
        assertEquals(timeInterval.timeStart(), TimeUtils.getFrom(TEST_TIMESTAMP_4[0]));
        assertEquals(timeInterval.timeEnd(), TimeUtils.getFrom(TEST_TIMESTAMP_4[4]));
    }

    @Test
    void getTagsData_whenDataRight_thenReturnTagList() throws InMemoryValidationStorageException {
        List<TagDataTimeIntervalDto> tagList = new ArrayList<>();
        tagList.add(createTagDataList(TEST_VALUE_DATA_RIGHT_FROM_ONE, TEST_TIMESTAMP_RIGHT_FROM_ONE,
                TAG_NAME_1, TAG_ID_1));
        tagList.add(createTagDataList(TEST_VALUE_DATA_RIGHT_FROM_ONE, TEST_TIMESTAMP_RIGHT_FROM_ONE,
                TAG_NAME_2, TAG_ID_2));

        List<TagDataTimeIntervalDto> result = dataStorage
                .getAndSaveTagsData(OBJECT_ID_1, TEST_TIMESTAMP_2[0], TEST_TIMESTAMP_2[4], tagList);

        assertFalse(tagList.isEmpty());
        assertEquals(result.get(0).getValues().size(), TEST_VALUE_DATA_RIGHT_RESULT.length);
        assertEquals(result.get(0).getTimestamps().size(), TEST_TIMESTAMP_RIGHT_RESULT.length);
        assertEquals(result.get(1).getValues().size(), TEST_VALUE_DATA_RIGHT_RESULT.length);
        assertEquals(result.get(1).getTimestamps().size(), TEST_TIMESTAMP_RIGHT_RESULT.length);
        assertTrue(result.get(0).getValues().containsAll(Arrays.asList(TEST_VALUE_DATA_RIGHT_RESULT)) &&
                Arrays.asList(TEST_VALUE_DATA_RIGHT_RESULT).containsAll(result.get(0).getValues()));
        assertTrue(result.get(1).getValues().containsAll(Arrays.asList(TEST_VALUE_DATA_RIGHT_RESULT)) &&
                Arrays.asList(TEST_VALUE_DATA_RIGHT_RESULT).containsAll(result.get(1).getValues()));
        assertTrue(result.get(0).getTimestamps().containsAll(Arrays.asList(TEST_TIMESTAMP_RIGHT_RESULT)) &&
                Arrays.asList(TEST_TIMESTAMP_RIGHT_RESULT).containsAll(result.get(0).getTimestamps()));
        assertTrue(result.get(1).getTimestamps().containsAll(Arrays.asList(TEST_TIMESTAMP_RIGHT_RESULT)) &&
                Arrays.asList(TEST_TIMESTAMP_RIGHT_RESULT).containsAll(result.get(1).getTimestamps()));
    }

    @Test
    void getTagsData_whenDataLeft_thenReturnTagList() throws InMemoryValidationStorageException {
        List<TagDataTimeIntervalDto> tagList = new ArrayList<>();
        tagList.add(createTagDataList(TEST_VALUE_DATA_LEFT_FROM_ONE, TEST_TIMESTAMP_LEFT_FROM_ONE,
                TAG_NAME_1, TAG_ID_1));
        tagList.add(createTagDataList(TEST_VALUE_DATA_LEFT_FROM_ONE, TEST_TIMESTAMP_LEFT_FROM_ONE,
                TAG_NAME_2, TAG_ID_2));

        List<TagDataTimeIntervalDto> result = dataStorage
                .getAndSaveTagsData(OBJECT_ID_1, TEST_TIMESTAMP_3[0], TEST_TIMESTAMP_3[4], tagList);

        assertFalse(tagList.isEmpty());
        assertEquals(result.get(0).getValues().size(), TEST_VALUE_DATA_LEFT_RESULT.length);
        assertEquals(result.get(0).getTimestamps().size(), TEST_TIMESTAMP_LEFT_RESULT.length);
        assertEquals(result.get(1).getValues().size(), TEST_VALUE_DATA_LEFT_RESULT.length);
        assertEquals(result.get(1).getTimestamps().size(), TEST_TIMESTAMP_LEFT_RESULT.length);
        assertTrue(result.get(0).getValues().containsAll(Arrays.asList(TEST_VALUE_DATA_LEFT_RESULT)) &&
                Arrays.asList(TEST_VALUE_DATA_LEFT_RESULT).containsAll(result.get(0).getValues()));
        assertTrue(result.get(1).getValues().containsAll(Arrays.asList(TEST_VALUE_DATA_LEFT_RESULT)) &&
                Arrays.asList(TEST_VALUE_DATA_LEFT_RESULT).containsAll(result.get(1).getValues()));
        assertTrue(result.get(0).getTimestamps().containsAll(Arrays.asList(TEST_TIMESTAMP_LEFT_RESULT)) &&
                Arrays.asList(TEST_TIMESTAMP_LEFT_RESULT).containsAll(result.get(0).getTimestamps()));
        assertTrue(result.get(1).getTimestamps().containsAll(Arrays.asList(TEST_TIMESTAMP_LEFT_RESULT)) &&
                Arrays.asList(TEST_TIMESTAMP_LEFT_RESULT).containsAll(result.get(1).getTimestamps()));
    }
}
