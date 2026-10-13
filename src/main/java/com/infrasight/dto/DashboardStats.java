package com.infrasight.dto;

public record DashboardStats(long monitored, long totalEndpoints, long healthy, long down, long openIncidents,
                             CheckSummary last24h) {
}
