package com.infrasight.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infrasight.dto.CheckSummary;
import com.infrasight.dto.EndpointReportRow;
import com.infrasight.dto.ReportView;
import com.infrasight.repository.CheckResultRepository;

@Service
public class ReportService {

    private final CheckResultRepository results;
    private final IncidentService incidents;

    public ReportService(CheckResultRepository results, IncidentService incidents) {
        this.results = results;
        this.incidents = incidents;
    }

    @Transactional(readOnly = true)
    public ReportView build(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<EndpointReportRow> rows = new ArrayList<>();
        for (Object[] r : results.reportByEndpoint(start, end)) {
            rows.add(new EndpointReportRow(
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    (String) r[2],
                    StatsMapper.number(r[3]),
                    StatsMapper.number(r[4]),
                    r[5] == null ? null : Math.round(((Number) r[5]).doubleValue() * 10.0) / 10.0,
                    r[6] == null ? null : ((Number) r[6]).longValue()));
        }

        CheckSummary overall = StatsMapper.summary(results.summary(start, end, 0L));

        List<EndpointReportRow> topFailing = rows.stream()
                .filter(row -> row.failed() > 0)
                .sorted(Comparator.comparingLong(EndpointReportRow::failed).reversed())
                .limit(5)
                .toList();

        return new ReportView(from, to, overall, rows, topFailing,
                incidents.summary(start, end), incidents.openedBetween(start, end, 25));
    }
}
