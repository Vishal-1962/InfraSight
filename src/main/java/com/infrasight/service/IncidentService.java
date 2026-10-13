package com.infrasight.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infrasight.dto.CheckOutcome;
import com.infrasight.dto.IncidentSummary;
import com.infrasight.entity.Incident;
import com.infrasight.entity.IncidentStatus;
import com.infrasight.entity.IncidentType;
import com.infrasight.entity.MonitoredEndpoint;
import com.infrasight.entity.Severity;
import com.infrasight.repository.IncidentRepository;

@Service
public class IncidentService {

    private static final List<IncidentStatus> ACTIVE = List.of(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED);

    private final IncidentRepository incidents;

    public IncidentService(IncidentRepository incidents) {
        this.incidents = incidents;
    }

    /**
     * Applies the threshold rules of an endpoint to a new check result. Runs inside the caller's
     * transaction and updates the counters on the managed endpoint. One incident is raised per failure streak.
     */
    @Transactional
    public void evaluate(MonitoredEndpoint endpoint, CheckOutcome outcome, LocalDateTime now) {
        int threshold = endpoint.getFailureThreshold();

        if (outcome.success()) {
            endpoint.setConsecutiveFailures(0);
            resolveAutomatically(endpoint, IncidentType.DOWN, now,
                    "Resolved automatically: the endpoint responded successfully.");
            endpoint.setDownAlertRaised(false);

            Integer limit = endpoint.getSlowThresholdMs();
            boolean slow = limit != null && outcome.responseTimeMs() != null && outcome.responseTimeMs() > limit;
            if (slow) {
                endpoint.setConsecutiveSlow(endpoint.getConsecutiveSlow() + 1);
                if (endpoint.getConsecutiveSlow() >= threshold && !endpoint.isSlowAlertRaised()) {
                    open(endpoint, IncidentType.SLOW, Severity.WARNING, now,
                            "Response time exceeded " + limit + " ms for " + endpoint.getConsecutiveSlow()
                                    + " consecutive checks (latest: " + outcome.responseTimeMs() + " ms).");
                    endpoint.setSlowAlertRaised(true);
                }
            } else {
                endpoint.setConsecutiveSlow(0);
                resolveAutomatically(endpoint, IncidentType.SLOW, now,
                        "Resolved automatically: response time is back within the limit.");
                endpoint.setSlowAlertRaised(false);
            }
        } else {
            endpoint.setConsecutiveSlow(0);
            endpoint.setConsecutiveFailures(endpoint.getConsecutiveFailures() + 1);
            if (endpoint.getConsecutiveFailures() >= threshold && !endpoint.isDownAlertRaised()) {
                String error = outcome.error() == null ? "no details" : outcome.error();
                open(endpoint, IncidentType.DOWN, Severity.CRITICAL, now,
                        "Failed " + endpoint.getConsecutiveFailures() + " consecutive checks. Last error: " + error);
                endpoint.setDownAlertRaised(true);
            }
        }
    }

    private void open(MonitoredEndpoint endpoint, IncidentType type, Severity severity, LocalDateTime now,
                      String message) {
        if (incidents.existsByEndpointIdAndTypeAndStatusNot(endpoint.getId(), type, IncidentStatus.RESOLVED)) {
            return;
        }
        Incident incident = new Incident();
        incident.setEndpoint(endpoint);
        incident.setType(type);
        incident.setSeverity(severity);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setMessage(truncate(message, 500));
        incident.setOpenedAt(now);
        incidents.save(incident);
    }

    private void resolveAutomatically(MonitoredEndpoint endpoint, IncidentType type, LocalDateTime now, String note) {
        for (Incident incident : incidents.findByEndpointIdAndTypeAndStatusNot(
                endpoint.getId(), type, IncidentStatus.RESOLVED)) {
            incident.setStatus(IncidentStatus.RESOLVED);
            incident.setResolvedAt(now);
            incident.setResolutionNote(note);
        }
    }

    @Transactional
    public void acknowledge(Long id) {
        Incident incident = incidents.findById(id).orElseThrow(() -> new NotFoundException("Incident not found."));
        if (incident.getStatus() == IncidentStatus.OPEN) {
            incident.setStatus(IncidentStatus.ACKNOWLEDGED);
            incident.setAcknowledgedAt(LocalDateTime.now());
        }
    }

    @Transactional
    public void resolve(Long id, String note) {
        Incident incident = incidents.findById(id).orElseThrow(() -> new NotFoundException("Incident not found."));
        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            incident.setStatus(IncidentStatus.RESOLVED);
            incident.setResolvedAt(LocalDateTime.now());
            String trimmed = note == null ? "" : note.trim();
            incident.setResolutionNote(trimmed.isEmpty() ? null : truncate(trimmed, 1000));
        }
    }

    @Transactional(readOnly = true)
    public Page<Incident> search(String filter, long endpointId, int page) {
        List<IncidentStatus> statuses = switch (filter) {
            case "open" -> List.of(IncidentStatus.OPEN);
            case "acknowledged" -> List.of(IncidentStatus.ACKNOWLEDGED);
            case "resolved" -> List.of(IncidentStatus.RESOLVED);
            case "all" -> List.of(IncidentStatus.values());
            default -> ACTIVE;
        };
        return incidents.search(statuses, endpointId, PageRequest.of(Math.max(page, 0), 25));
    }

    @Transactional(readOnly = true)
    public List<Incident> recent(int limit) {
        return incidents.recent(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<Incident> forEndpoint(Long endpointId) {
        return incidents.findTop5ByEndpointIdOrderByOpenedAtDesc(endpointId);
    }

    @Transactional(readOnly = true)
    public long activeCount() {
        return incidents.countByStatusIn(ACTIVE);
    }

    @Transactional(readOnly = true)
    public IncidentSummary summary(LocalDateTime from, LocalDateTime to) {
        long total = 0;
        long critical = 0;
        long warning = 0;
        long open = 0;
        long acknowledged = 0;
        long resolved = 0;
        for (Object[] row : incidents.countsBetween(from, to)) {
            Severity severity = (Severity) row[0];
            IncidentStatus status = (IncidentStatus) row[1];
            long count = ((Number) row[2]).longValue();
            total += count;
            if (severity == Severity.CRITICAL) {
                critical += count;
            } else {
                warning += count;
            }
            if (status == IncidentStatus.OPEN) {
                open += count;
            } else if (status == IncidentStatus.ACKNOWLEDGED) {
                acknowledged += count;
            } else {
                resolved += count;
            }
        }
        return new IncidentSummary(total, critical, warning, open, acknowledged, resolved);
    }

    @Transactional(readOnly = true)
    public List<Incident> openedBetween(LocalDateTime from, LocalDateTime to, int limit) {
        return incidents.openedBetween(from, to, PageRequest.of(0, limit));
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
