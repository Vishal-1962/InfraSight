package com.infrasight.dto;

import java.time.LocalDate;
import java.util.List;

import com.infrasight.entity.Incident;

public record ReportView(LocalDate from, LocalDate to, CheckSummary overall, List<EndpointReportRow> rows,
                         List<EndpointReportRow> topFailing, IncidentSummary incidents,
                         List<Incident> incidentList) {
}
