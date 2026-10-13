package com.infrasight.dto;

public record CheckSummary(long total, long success, Double avgMs, Long maxMs) {

    public static final CheckSummary EMPTY = new CheckSummary(0, 0, null, null);

    public long failed() {
        return total - success;
    }

    /** Percentage 0-100, or null when there are no checks. */
    public Double availability() {
        return total == 0 ? null : Math.round(success * 10000.0 / total) / 100.0;
    }
}
