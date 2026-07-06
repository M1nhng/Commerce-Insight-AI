package com.commerceinsight.shared.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * DateUtil — shared date/time utility methods used across modules.
 *
 * <p>Architecture Rule: All timestamps in the system are UTC.
 * Never use LocalDate/LocalDateTime in persistence; always use Instant.
 * Use this utility for formatting and converting for display purposes only.
 */
public final class DateUtil {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter DATE_ONLY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtil() {
        // Utility class — prevent instantiation
    }

    /**
     * Format an Instant as an ISO 8601 string in UTC.
     *
     * @param instant the instant to format
     * @return ISO 8601 formatted string (e.g., "2026-07-06T10:00:00Z"), or null if input is null
     */
    public static String formatUtc(Instant instant) {
        if (instant == null) return null;
        return ISO_FORMATTER.format(instant.atZone(ZoneOffset.UTC));
    }

    /**
     * Format an Instant as a date-only string (yyyy-MM-dd) in UTC.
     *
     * @param instant the instant to format
     * @return date string (e.g., "2026-07-06"), or null if input is null
     */
    public static String formatDateOnly(Instant instant) {
        if (instant == null) return null;
        return DATE_ONLY_FORMATTER.format(instant.atZone(ZoneOffset.UTC));
    }

    /**
     * Get the start of the current day in UTC (midnight).
     */
    public static Instant startOfToday() {
        return LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Get the start of a specific date in UTC.
     *
     * @param date the local date
     * @return midnight UTC for that date
     */
    public static Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Get the end of a specific date in UTC (last nanosecond).
     *
     * @param date the local date
     * @return 23:59:59.999999999 UTC for that date
     */
    public static Instant endOf(LocalDate date) {
        return date.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
    }

    /**
     * Calculate the number of days between two instants.
     *
     * @param from start instant
     * @param to   end instant
     * @return number of days between the two instants
     */
    public static long daysBetween(Instant from, Instant to) {
        return Duration.between(from, to).toDays();
    }

    /**
     * Check if an instant is in the past.
     *
     * @param instant the instant to check
     * @return true if the instant is before now
     */
    public static boolean isPast(Instant instant) {
        return instant != null && Instant.now().isAfter(instant);
    }
}
