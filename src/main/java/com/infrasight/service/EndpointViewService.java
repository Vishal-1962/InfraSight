package com.infrasight.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infrasight.dto.Beat;
import com.infrasight.dto.CheckSummary;
import com.infrasight.dto.EndpointView;
import com.infrasight.entity.CheckResult;
import com.infrasight.entity.MonitoredEndpoint;
import com.infrasight.repository.CheckResultRepository;

@Service
public class EndpointViewService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CheckResultRepository results;

    public EndpointViewService(CheckResultRepository results) {
        this.results = results;
    }

    @Transactional(readOnly = true)
    public List<EndpointView> build(List<MonitoredEndpoint> endpoints, int beatCount) {
        LocalDateTime now = LocalDateTime.now();
        List<EndpointView> views = new ArrayList<>();
        for (MonitoredEndpoint endpoint : endpoints) {
            List<CheckResult> recent = new ArrayList<>(
                    results.recentForEndpoint(endpoint.getId(), PageRequest.of(0, beatCount)));
            Collections.reverse(recent); // oldest first, newest on the right

            List<Beat> beats = new ArrayList<>();
            for (int i = recent.size(); i < beatCount; i++) {
                beats.add(new Beat("none", "No check yet"));
            }
            for (CheckResult c : recent) {
                String detail = c.isSuccess()
                        ? "HTTP " + c.getStatusCode() + ", " + c.getResponseTimeMs() + " ms"
                        : (c.getErrorMessage() == null ? "Failed" : c.getErrorMessage());
                beats.add(new Beat(c.isSuccess() ? "ok" : "fail", c.getCheckedAt().format(TIME) + " - " + detail));
            }

            CheckSummary day = StatsMapper.summary(
                    results.summary(now.minusHours(24), now.plusMinutes(1), endpoint.getId()));
            views.add(new EndpointView(endpoint, beats, day.availability(), day.avgMs()));
        }
        return views;
    }
}
