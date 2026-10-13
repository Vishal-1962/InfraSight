package com.infrasight.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infrasight.dto.CheckOutcome;
import com.infrasight.entity.CheckResult;
import com.infrasight.entity.EndpointStatus;
import com.infrasight.entity.MonitoredEndpoint;
import com.infrasight.repository.CheckResultRepository;
import com.infrasight.repository.EndpointRepository;

/** Stores a check result and updates the endpoint state and incidents in one transaction. */
@Service
public class CheckRecorder {

    private final EndpointRepository endpoints;
    private final CheckResultRepository results;
    private final IncidentService incidents;

    public CheckRecorder(EndpointRepository endpoints, CheckResultRepository results, IncidentService incidents) {
        this.endpoints = endpoints;
        this.results = results;
        this.incidents = incidents;
    }

    /** Returns the stored result, or null if the endpoint was deleted while the check was running. */
    @Transactional
    public CheckResult record(Long endpointId, CheckOutcome outcome) {
        MonitoredEndpoint endpoint = endpoints.findById(endpointId).orElse(null);
        if (endpoint == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();

        CheckResult result = new CheckResult();
        result.setEndpoint(endpoint);
        result.setCheckedAt(now);
        result.setSuccess(outcome.success());
        result.setStatusCode(outcome.statusCode());
        result.setResponseTimeMs(outcome.responseTimeMs());
        String error = outcome.error();
        result.setErrorMessage(error == null ? null : (error.length() > 500 ? error.substring(0, 499) + "…" : error));
        results.save(result);

        endpoint.setLastCheckedAt(now);
        endpoint.setLastResponseTimeMs(outcome.responseTimeMs());
        endpoint.setLastStatusCode(outcome.statusCode());
        endpoint.setCurrentStatus(outcome.success() ? EndpointStatus.UP : EndpointStatus.DOWN);
        incidents.evaluate(endpoint, outcome, now);
        endpoints.save(endpoint);
        return result;
    }
}
