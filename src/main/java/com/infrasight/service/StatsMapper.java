package com.infrasight.service;

import java.util.List;

import com.infrasight.dto.CheckSummary;

final class StatsMapper {

    private StatsMapper() {
    }

    /** Converts the single row returned by CheckResultRepository.summary into a CheckSummary. */
    static CheckSummary summary(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return CheckSummary.EMPTY;
        }
        Object[] row = rows.get(0);
        long total = number(row[0]);
        if (total == 0) {
            return CheckSummary.EMPTY;
        }
        long success = number(row[1]);
        Double avg = row[2] == null ? null : Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0;
        Long max = row[3] == null ? null : ((Number) row[3]).longValue();
        return new CheckSummary(total, success, avg, max);
    }

    static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
