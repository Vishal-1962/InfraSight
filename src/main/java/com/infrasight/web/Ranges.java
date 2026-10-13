package com.infrasight.web;

import java.time.LocalDate;

/** Resolves optional from/to dates from a filter form into a valid inclusive range. */
final class Ranges {

    private Ranges() {
    }

    /** Defaults to the last {@code defaultDays} days (including today); swaps the dates if reversed. */
    static LocalDate[] resolve(LocalDate from, LocalDate to, int defaultDays) {
        LocalDate today = LocalDate.now();
        LocalDate end = to;
        LocalDate start = from;
        if (start == null && end == null) {
            end = today;
            start = today.minusDays(defaultDays - 1L);
        } else if (end == null) {
            end = start.isAfter(today) ? start : today;
        } else if (start == null) {
            start = end.minusDays(defaultDays - 1L);
        }
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        return new LocalDate[] {start, end};
    }
}
