package com.infrasight.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infrasight.dto.CheckSummary;
import com.infrasight.dto.DashboardStats;
import com.infrasight.entity.CheckResult;
import com.infrasight.entity.EndpointStatus;
import com.infrasight.repository.CheckResultRepository;
import com.infrasight.repository.EndpointRepository;

@Service
public class DashboardService {

    private final EndpointRepository endpoints;
    private final CheckResultRepository results;
    private final IncidentService incidents;

    public DashboardService(EndpointRepository endpoints, CheckResultRepository results, IncidentService incidents) {
        this.endpoints = endpoints;
        this.results = results;
        this.incidents = incidents;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        LocalDateTime now = LocalDateTime.now();
        CheckSummary last24h = StatsMapper.summary(results.summary(now.minusHours(24), now.plusMinutes(1), 0L));
        return new DashboardStats(
                endpoints.countByEnabledTrue(),
                endpoints.count(),
                endpoints.countByEnabledTrueAndCurrentStatus(EndpointStatus.UP),
                endpoints.countByEnabledTrueAndCurrentStatus(EndpointStatus.DOWN),
                incidents.activeCount(),
                last24h);
    }

    @Transactional(readOnly = true)
    public List<CheckResult> recentChecks(int limit) {
        return results.recent(PageRequest.of(0, limit));
    }
}
