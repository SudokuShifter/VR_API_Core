package com.inlinegroup.vrcalculationbackend.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(value = "cma-config")
@Slf4j
public class VRCalcConfig {
    public static final int DEFAULT_PORT = -1;
    public static final int DEFAULT_NUMBER_OF_RE_REQUEST_IN_WINDOW_SCHEDULER = 1;
    public static final int DEFAULT_COUNT_BAD_TAG_VALUE_FOR_MAIN_OBJECT = 0;

    public static final String COMMON_RESPONSE_ON_500_ERROR = "Server is not available";
    public static final String SERVICE_REGISTRATION_ID = "service-auth";

    public static final String ZIF_TEMPERATURE_CUR_NAME = "температур";
    public static final Double FACTOR_TEMPERATURE_KELVIN = 273.15;

    public static final String ZIF_OM_QP_ONLY_ROOT = "onlyRoot";
    public static final String ZIF_OM_QP_INCLUDE_DELETED = "includeDeleted";
    public static final String ZIF_OM_QP_CALCULATE_TOTAL_COUNT = "calculateTotalCount";
    public static final String ZIF_OM_QP_FILTER_MODE = "filterMode";
    public static final String ZIF_OM_QP_OBJECT_ID = "objectId";
    public static final String ZIF_OM_QP_NAME = "name";

    public static final String ZIF_UDL_QP_TIME_LEFT = "timeLeft";
    public static final String ZIF_UDL_QP_TIME_RIGHT = "timeRight";
    public static final String ZIF_UDL_QP_TIME = "time";
    public static final String ZIF_UDL_QP_VALUE = "value";
    public static final String ZIF_UDL_QP_ONLY_GOOD_VALUE = "onlyGoodValues";

    public static final int ZIF_UID_LENGTH = 36;
    public static final int TIME_STEP = 30;
    public static final String ML_TARGET_NAME_GAS_CONSUMPTION = "Расход по газу";

    public static final String ZIF_CONST_DATA_OBJECT_NAME = "вспомогательные данные";
    public static final String ZIF_MANIFOLD_OBJECT_NAME = "манифольд";
    public static final String ZIF_FMM_OBJECT_NAME = "фмм";
    public static final String ZIF_ML_OBJECT_NAME = "ml";

    public static final String ZIF_TAG_NAME_GAMMA_GAS = "относительная плотность газа";
    public static final String ZIF_TAG_NAME_GAMMA_GC = "относительная плотность газоконденсата";
    public static final String ZIF_TAG_NAME_GAMMA_WAT = "относительная плотность воды";
    public static final String ZIF_TAG_NAME_D_TUBE = "диаметр трубы выкидной линии";

    public static final String ZIF_TAG_NAME_Q_GAS = "расход по газу вентури";
    public static final String ZIF_TAG_NAME_Q_GS = "расход по конденсату вентури";
    public static final String ZIF_TAG_NAME_Q_WAT = "расход по воде вентури";

    public static final String ZIF_TAG_NAME_D_CHOKE_PERCENT_TIMED = "процент открытия штуцера";
    public static final String ZIF_TAG_NAME_P_BUF = "давление над буферной задвижкой фа";
    public static final String ZIF_TAG_NAME_T_BUF = "температура на выкидной линии";

    public static final String ZIF_TAG_NAME_P_DOWN_HOLE = "давление забойное";
    public static final String ZIF_TAG_NAME_T_DOWN_HOLE = "температура забойная";

    public static final String ZIF_MANIFOLD_TAG_NAME_P_OUT = "давление";
    public static final String ZIF_MANIFOLD_TAG_NAME_T_OUT = "температура";

    public static final String ZIF_FMM_TAG_NAME_Q_GAS = "дебит газа";
    public static final String ZIF_FMM_TAG_NAME_Q_GC = "дебит газоконденсата";
    public static final String ZIF_FMM_TAG_NAME_Q_WAT = "дебит воды";
    public static final String ZIF_FMM_TAG_NAME_ERROR_GAS = "относительная ошибка по газу";
    public static final String ZIF_FMM_TAG_NAME_ERROR_GC = "относительная ошибка по газоконденсату";
    public static final String ZIF_FMM_TAG_NAME_ERROR_WAT = "относительная ошибка по воде";
    public static final String ZIF_ML_TAG_NAME_GAS_CONSUMPTION = "расход по газу";
    public static final String ZIF_ML_TAG_NAME_GAS_ERROR = "относительная ошибка по газу";

    public static final String ZIF_FMM_TAG_NAME_GAS_CONDENSATE_FACTOR = "газоконденсатный фактор";
    public static final String ZIF_FMM_TAG_NAME_WATER_CUT = "обводненность";

    public static final String TIME_FORMAT_UTC_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private String zifScheme;
    private String zifHost;
    private String uriOmObjectGetAllObjectsByModel;
    private String uriOmObjectGetIncludeObjectsByModel;
    private String uriOmObjectGetObjectById;
    private String uriOmObjectGetProperties;
    private String uriOmObjectGetProperty;

    private String uriOmUomGetUomById;

    private String uriUdlDfaWebApiGetValue;
    private String uriUdlDfaWebApiGetValues;
    private String uriUdlDfaWebApiGetNext;
    private String uriUdlDfaWebApiWriteValue;

    private String fmmScheme;
    private String fmmHost;
    private Integer fmmPort = DEFAULT_PORT;
    private String fmmUriCalcFmmTask;
    private String fmmUriCalcAdaptTask;
    private String fmmUriCalcValidateTask;

    private String vrTypeScheduler;
    private String vrTaskDateStart;
    private String vrTaskSchedulerCron;
    private Integer vrTaskValidationCountDays;
    private Integer vrTaskSchedulerDeepValidation;
    private Integer vrTaskSchedulerCheckStepAfter;

    private String vrCfSchedulerCron;

    private String mlScheme;
    private String mlHost;
    private Integer mlPort = DEFAULT_PORT;
    private String mlUriCalcMlTask;
    private String mlUriAvailabilityOfModel;
    private String mlUriAvailabilityOfService;

    private int cores = Runtime.getRuntime().availableProcessors();

    public TypeScheduler getTypeScheduler() {
        if (TypeScheduler.contains(vrTypeScheduler)) {
            return TypeScheduler.valueOf(vrTypeScheduler);
        }
        log.error("The scheduler type is incorrectly set in the configuration file. The default value is set.");
        return TypeScheduler.DATA_FOUND;
    }
}