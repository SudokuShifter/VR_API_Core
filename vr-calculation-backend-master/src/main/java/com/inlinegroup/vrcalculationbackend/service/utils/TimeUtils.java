package com.inlinegroup.vrcalculationbackend.service.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.TIME_FORMAT_UTC_ISO8601;
import static com.inlinegroup.vrcalculationbackend.config.VRCalcConfig.TIME_STEP;

public class TimeUtils {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_FORMAT_UTC_ISO8601);

    private TimeUtils() {
    }

    public static String getCurrentTime() {
        LocalDateTime time = LocalDateTime.now();
        return time.format(formatter);
    }

    public static String toString(LocalDateTime time) {
        return time.format(formatter);
    }

    public static String getCurrentTimeMinusRound() {
        LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        return time.format(formatter);
    }

    public static String getTimeMinusDays(String time, int dayCount) {
        LocalDateTime curTime = LocalDateTime.parse(time, formatter);
        curTime = curTime.minusDays(dayCount);
        return curTime.format(formatter);
    }

    public static String getTimeMinusDays(String time, int dayCount, int minusSeconds) {
        LocalDateTime curTime = LocalDateTime.parse(time, formatter);
        curTime = curTime.minusDays(dayCount).minusSeconds(minusSeconds);
        return curTime.format(formatter);
    }

    public static String roundTime(String time) {
        LocalDateTime curTime = LocalDateTime.parse(time, formatter).truncatedTo(ChronoUnit.MINUTES);
        return curTime.format(formatter);
    }

    public static LocalDateTime getFrom(String time) {
        return LocalDateTime.parse(time, formatter);
    }

    public static List<String> getTimeList(String timeLeft, String timeRight) {
        LocalDateTime curTime = TimeUtils.getFrom(timeLeft);
        LocalDateTime timeEnd = TimeUtils.getFrom(timeRight);
        List<String> dateList = new ArrayList<>();
        while (curTime.isBefore(timeEnd) || curTime.equals(timeEnd)) {
            dateList.add(TimeUtils.toString(curTime));
            curTime = curTime.plusSeconds(TIME_STEP);
        }
        return dateList;
    }

    /**
     * Метод корректировки даты. Т.к. В платформе "цифра" присутствует ошибка в запросе архивных данных тега
     * (при указании конечной даты кратной месяцу - значение для этой даты не возвращается) необходимо уменьшить
     * последнюю дату месяца на временной шаг платформы.
     *
     * @param date - передаваемая дата
     */
    public static String correctEndDate(String date) {
        LocalDateTime curDate = LocalDateTime.parse(date, formatter);
        if (curDate.getDayOfMonth() == 1 && curDate.getHour() == 0 && curDate.getMinute() == 0 &&
                curDate.getSecond() == 0) {
            curDate = curDate.plusSeconds(TIME_STEP);
        }
        return toString(curDate);
    }

    public static LocalDateTime correctEndDate(LocalDateTime date) {
        LocalDateTime curDate = date;
        if (curDate.getDayOfMonth() == 1 && curDate.getHour() == 0 && curDate.getMinute() == 0 &&
                curDate.getSecond() == 0) {
            curDate = curDate.plusSeconds(TIME_STEP);
        }
        return curDate;
    }
}
