package com.hallbooking.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * DateTimeUtils — all date/time helpers used across the app.
 * Standard format: "yyyy-MM-dd HH:mm"
 */
public class DateTimeUtils {

    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static final DateTimeFormatter DATE_ONLY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Current timestamp as formatted string ──────────────────────
    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static String today() {
        return LocalDate.now().format(DATE_ONLY);
    }

    // ── Parse string to LocalDateTime ─────────────────────────────
    public static LocalDateTime parse(String datetime) {
        return LocalDateTime.parse(datetime, FORMATTER);
    }

    // ── Format LocalDateTime to string ────────────────────────────
    public static String format(LocalDateTime dt) {
        return dt.format(FORMATTER);
    }

    // ── Hours between two datetime strings ────────────────────────
    public static double hoursBetween(String start, String end) {
        LocalDateTime s = parse(start);
        LocalDateTime e = parse(end);
        return (double) ChronoUnit.MINUTES.between(s, e) / 60.0;
    }

    // ── Days between today and a future datetime string ────────────
    public static long daysUntil(String futureDatetime) {
        LocalDateTime future = parse(futureDatetime);
        return ChronoUnit.DAYS.between(LocalDateTime.now(), future);
    }

    // ── Check if a date string is in the past ─────────────────────
    public static boolean isInPast(String datetime) {
        return parse(datetime).isBefore(LocalDateTime.now());
    }

    // ── Check if two time ranges overlap ──────────────────────────
    // True if [start1, end1) and [start2, end2) share any time
    public static boolean overlaps(String start1, String end1,
                                    String start2, String end2) {
        LocalDateTime s1 = parse(start1), e1 = parse(end1);
        LocalDateTime s2 = parse(start2), e2 = parse(end2);
        return s1.isBefore(e2) && e1.isAfter(s2);
    }

    // ── Check if datetime is within operating hours 08:00–18:00 ───
    public static boolean isWithinOperatingHours(String start, String end) {
        LocalDateTime s = parse(start);
        LocalDateTime e = parse(end);
        int openHour  = 8;
        int closeHour = 18;
        return s.getHour() >= openHour
                && e.getHour() <= closeHour
                && e.isAfter(s)
                && s.toLocalDate().equals(e.toLocalDate()); // same day
    }

    // ── Receipt number generator ───────────────────────────────────
    public static String generateReceiptNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timePart = String.valueOf(System.currentTimeMillis() % 10000);
        return "REC-" + datePart + "-" + String.format("%04d", Integer.parseInt(timePart));
    }

    // ── Readable display format  e.g. "21 Mar 2026, 09:00" ────────
    public static String toDisplayFormat(String datetime) {
        try {
            LocalDateTime dt = parse(datetime);
            return dt.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"));
        } catch (Exception e) {
            return datetime;
        }
    }
}
