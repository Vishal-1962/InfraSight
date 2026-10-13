package com.infrasight.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infrasight.dto.TrendPoint;
import com.infrasight.repository.CheckResultRepository;

/** Builds hourly (or daily, for long ranges) response-time and availability series from stored checks. */
@Service
public class TrendService {

    private static final DateTimeFormatter HOUR_ONLY = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_HOUR = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_ONLY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private final CheckResultRepository results;
    private final ObjectMapper objectMapper;

    public TrendService(CheckResultRepository results, ObjectMapper objectMapper) {
        this.results = results;
        this.objectMapper = objectMapper;
    }

    private static final class Bucket {
        long checks;
        long success;
        long responseSum;
        long responseCount;
    }

    /**
     * Series for checks with from <= checkedAt < to. endpointId 0 means all endpoints.
     * Ranges longer than three days are grouped by day, shorter ranges by hour.
     */
    @Transactional(readOnly = true)
    public List<TrendPoint> trend(LocalDateTime from, LocalDateTime to, long endpointId) {
        Duration span = Duration.between(from, to);
        boolean daily = span.toHours() > 72;
        ChronoUnit unit = daily ? ChronoUnit.DAYS : ChronoUnit.HOURS;

        Map<LocalDateTime, Bucket> buckets = new LinkedHashMap<>();
        LocalDateTime cursor = from.truncatedTo(unit);
        boolean fill = (daily ? span.toDays() : span.toHours()) <= 800;
        if (fill) {
            while (cursor.isBefore(to)) {
                buckets.put(cursor, new Bucket());
                cursor = cursor.plus(1, unit);
            }
        }

        for (Object[] row : results.hourlyBuckets(from, to, endpointId)) {
            LocalDateTime hour = LocalDateTime.of(
                    ((Number) row[0]).intValue(), ((Number) row[1]).intValue(),
                    ((Number) row[2]).intValue(), ((Number) row[3]).intValue(), 0);
            LocalDateTime key = daily ? hour.truncatedTo(ChronoUnit.DAYS) : hour;
            Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
            bucket.checks += StatsMapper.number(row[4]);
            bucket.success += StatsMapper.number(row[5]);
            bucket.responseSum += StatsMapper.number(row[6]);
            bucket.responseCount += StatsMapper.number(row[7]);
        }

        List<Map.Entry<LocalDateTime, Bucket>> entries = new ArrayList<>(buckets.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        boolean multiDay = span.toHours() > 24;

        List<TrendPoint> points = new ArrayList<>();
        for (Map.Entry<LocalDateTime, Bucket> entry : entries) {
            Bucket b = entry.getValue();
            DateTimeFormatter format = daily ? DAY_ONLY : (multiDay ? DAY_HOUR : HOUR_ONLY);
            Double avg = b.responseCount == 0 ? null : Math.round(b.responseSum * 10.0 / b.responseCount) / 10.0;
            Double availability = b.checks == 0 ? null : Math.round(b.success * 10000.0 / b.checks) / 100.0;
            points.add(new TrendPoint(entry.getKey().format(format), avg, availability, b.checks));
        }
        return points;
    }

    public String toJson(List<TrendPoint> points) {
        try {
            return objectMapper.writeValueAsString(points);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    public static boolean hasData(List<TrendPoint> points) {
        return points.stream().anyMatch(p -> p.checks() > 0);
    }
}
