package com.infrasight.dto;

import java.util.List;

import com.infrasight.entity.MonitoredEndpoint;

/** An endpoint with its recent heartbeat bars and 24 hour figures, for list and dashboard pages. */
public record EndpointView(MonitoredEndpoint endpoint, List<Beat> beats, Double uptime, Double avgMs) {
}
