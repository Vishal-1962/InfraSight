package com.infrasight.dto;

public record IncidentSummary(long total, long critical, long warning, long open, long acknowledged, long resolved) {
}
