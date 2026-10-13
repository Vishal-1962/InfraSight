package com.infrasight.dto;

public record EndpointReportRow(Long id, String name, String url, long total, long success, Double avgMs, Long maxMs) {

    public long failed() {
        return total - success;
    }

    public Double availability() {
        return total == 0 ? null : Math.round(success * 10000.0 / total) / 100.0;
    }
}
