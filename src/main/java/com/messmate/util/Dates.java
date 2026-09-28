package com.messmate.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/** Date helpers so the whole app agrees on "today" and on what a week is (Monday to Sunday). */
public final class Dates {
    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private Dates() {}

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** Monday of the week containing d (or of the current week when d is null). */
    public static LocalDate weekStart(LocalDate d) {
        return (d == null ? today() : d).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
