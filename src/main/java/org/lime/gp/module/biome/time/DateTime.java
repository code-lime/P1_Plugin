package org.lime.gp.module.biome.time;

import org.apache.commons.lang.StringUtils;
import org.lime.gp.lime;

import java.util.Optional;

public class DateTime {
    public static final DateTime START_TIME = new DateTime(0).addYears(183);

    private final long totalSeconds;

    private DateTime(long totalSeconds) { this.totalSeconds = totalSeconds; }

    public static DateTime ofHours(double totalHours) { return new DateTime(Math.round(totalHours * 3600)); }

    public int getYear() { return (int) getRoundYears() + 1000; }
    public int getSeasonIndex() { return (int) (getRoundYears() % SEASON_IN_YEARS) + 1; }
    public SeasonKey getSeasonKey() { return SeasonKey.byIndex(getSeasonIndex()); }
    public int getDay() { return (int) (getRoundDays() % YEAR_TO_DAYS) + 1; }
    public int getHour() { return (int) (getRoundHours() % DAY_TO_HOURS); }
    public int getMinute() { return (int) (getRoundMinutes() % HOUR_TO_MINUTES); }
    public int getSecond() { return (int) (getRoundSeconds() % MINUTE_TO_SECONDS); }

    public static final int MINUTE_TO_SECONDS = 60;
    public static final int HOUR_TO_MINUTES = 60;
    public static final int DAY_TO_HOURS = 24;
    public static final int YEAR_TO_DAYS = 90;
    public static final int SEASON_IN_YEARS = 3;

    public long getRoundYears() { return getRoundDays() / YEAR_TO_DAYS; }
    public long getRoundDays() { return getRoundHours() / DAY_TO_HOURS; }
    public long getRoundHours() { return getRoundMinutes() / HOUR_TO_MINUTES; }
    public long getRoundMinutes() { return getRoundSeconds() / MINUTE_TO_SECONDS; }
    public long getRoundSeconds() { return totalSeconds; }

    public double getTotalYears() { return getTotalDays() / YEAR_TO_DAYS; }
    public double getTotalDays() { return getTotalHours() / DAY_TO_HOURS; }
    public double getTotalHours() { return getTotalMinutes() / HOUR_TO_MINUTES; }
    public double getTotalMinutes() { return getTotalSeconds() / MINUTE_TO_SECONDS; }
    public double getTotalSeconds() { return totalSeconds; }

    public DateTime addYears(double value) { return addDays(value * YEAR_TO_DAYS); }
    public DateTime addDays(double value) { return addHours(value * DAY_TO_HOURS); }
    public DateTime addHours(double value) { return addMinutes(value * HOUR_TO_MINUTES); }
    public DateTime addMinutes(double value) { return addSeconds(value * MINUTE_TO_SECONDS); }
    public DateTime addSeconds(double value) { return new DateTime(Math.round(totalSeconds + value)); }

    public DateTime parse(String value) { return throwParse(value); }

    private static String appendFormat(String output, String prefix, int value) {
        return output
                .replace(prefix + prefix, StringUtils.leftPad(String.valueOf(value), 2, '0'))
                .replace(prefix, String.valueOf(value));
    }

    public String toFormat(String format) {
        long totalSeconds = this.totalSeconds;
        long totalMinutes = totalSeconds / MINUTE_TO_SECONDS;
        long totalHours = totalMinutes / HOUR_TO_MINUTES;
        long totalDays = totalHours / DAY_TO_HOURS;
        long totalYears = totalDays / YEAR_TO_DAYS;
        int year = (int) totalYears + 1000;
        int season = (int) (totalYears % SEASON_IN_YEARS) + 1;
        int day = (int) (totalDays % YEAR_TO_DAYS) + 1;
        int hour = (int) (totalHours % DAY_TO_HOURS);
        int minute = (int) (totalMinutes % HOUR_TO_MINUTES);
        int second = (int) (totalSeconds % MINUTE_TO_SECONDS);

/*
yy: Year, (e.g. 2015 would be 15)
yyyy: Year, (e.g. 2015)

S: Season number (eg. 3). From 1 through 3.
SS: Season named key (e.g. R). S - Sun, F - Frozen, R - Rain.

d: Represents the day of the month as a number from 1 through 31.
dd: Represents the day of the month as a number from 01 through 31.

H: 24-hour clock hour (e.g. 15). From 1 through 24.
HH: 24-hour clock hour, with a leading 0 (e.g. 22). From 01 through 24.

m: Minutes. From 1 through 60.
mm: Minutes with a leading zero. From 01 through 60.

s: Seconds
ss: Seconds with leading zero
        */

        format = format.replace("yyyy", String.valueOf(year));
        format = format.replace("yy", StringUtils.leftPad(String.valueOf(year % 100), 2, '0'));

        format = format.replace("SS", SeasonKey.byIndex(season).prefixString);
        //format = format.replace("S", String.valueOf(season));

        format = appendFormat(format, "d", day);
        format = appendFormat(format, "H", hour);
        format = appendFormat(format, "m", minute);
        format = appendFormat(format, "s", second);

        return format;
    }

    @Override public String toString() { return toFormat("yyyy-dd HH:mm:ss"); }

    private static long parseDate(String value) {
        String[] parts = value.split("-");
        if (parts.length != 2) throw new IllegalArgumentException("Date '"+value+"' not formatted by 'yyyy-dd'");
        long years = Integer.parseInt(parts[0]) - 1000;
        long days = Integer.parseInt(parts[1]) - 1;
        return (years * YEAR_TO_DAYS + days) * DAY_TO_HOURS * HOUR_TO_MINUTES * MINUTE_TO_SECONDS;
    }
    private static long parseTime(String value) {
        String[] parts = value.split(":");
        if (parts.length != 3) throw new IllegalArgumentException("Date '"+value+"' not formatted by 'HH:mm:ss'");
        long hours = Integer.parseInt(parts[0]);
        long minutes = Integer.parseInt(parts[1]);
        long seconds = Integer.parseInt(parts[2]);
        return (hours * HOUR_TO_MINUTES + minutes) * MINUTE_TO_SECONDS + seconds;
    }

    public static DateTime throwParse(String value) {
        String[] parts = value.trim().split(" ");
        if (parts.length == 1) {
            String part = parts[0];
            return new DateTime(part.contains("-") ? parseDate(part) : parseTime(part));
        }
        return new DateTime(parts[0].contains("-")
                ? (parseDate(parts[0]) + parseTime(parts[1]))
                : (parseDate(parts[1]) + parseTime(parts[0]))
        );
    }
    public static Optional<DateTime> tryParse(String value) {
        try {
            return Optional.of(throwParse(value));
        } catch (Exception e) {
            lime.logOP("Error parse DateTime from '"+value+"'. Supported format: 'yyyy-SS-dd HH:mm:ss'");
            return Optional.empty();
        }
    }
}








