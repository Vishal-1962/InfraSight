package com.infrasight.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "monitored_endpoint")
public class MonitoredEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2000)
    private String url;

    @Column(nullable = false)
    private int timeoutSeconds = 10;

    @Column(nullable = false)
    private int intervalSeconds = 60;

    /** Expected HTTP status. Null means any 2xx or 3xx response is healthy. */
    private Integer expectedStatus;

    /** Consecutive bad checks before an incident is raised. */
    @Column(nullable = false)
    private int failureThreshold = 3;

    /** Responses slower than this (ms) count as slow. Null disables the slow-response rule. */
    private Integer slowThresholdMs;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EndpointStatus currentStatus = EndpointStatus.UNKNOWN;

    @Column(nullable = false)
    private int consecutiveFailures = 0;

    @Column(nullable = false)
    private int consecutiveSlow = 0;

    @Column(nullable = false)
    private boolean downAlertRaised = false;

    @Column(nullable = false)
    private boolean slowAlertRaised = false;

    private LocalDateTime lastCheckedAt;

    private Long lastResponseTimeMs;

    private Integer lastStatusCode;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public Integer getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(Integer expectedStatus) { this.expectedStatus = expectedStatus; }
    public int getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
    public Integer getSlowThresholdMs() { return slowThresholdMs; }
    public void setSlowThresholdMs(Integer slowThresholdMs) { this.slowThresholdMs = slowThresholdMs; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public EndpointStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(EndpointStatus currentStatus) { this.currentStatus = currentStatus; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
    public int getConsecutiveSlow() { return consecutiveSlow; }
    public void setConsecutiveSlow(int consecutiveSlow) { this.consecutiveSlow = consecutiveSlow; }
    public boolean isDownAlertRaised() { return downAlertRaised; }
    public void setDownAlertRaised(boolean downAlertRaised) { this.downAlertRaised = downAlertRaised; }
    public boolean isSlowAlertRaised() { return slowAlertRaised; }
    public void setSlowAlertRaised(boolean slowAlertRaised) { this.slowAlertRaised = slowAlertRaised; }
    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
    public Long getLastResponseTimeMs() { return lastResponseTimeMs; }
    public void setLastResponseTimeMs(Long lastResponseTimeMs) { this.lastResponseTimeMs = lastResponseTimeMs; }
    public Integer getLastStatusCode() { return lastStatusCode; }
    public void setLastStatusCode(Integer lastStatusCode) { this.lastStatusCode = lastStatusCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Label used in the UI. */
    public String getStatusLabel() {
        if (!enabled) {
            return "Disabled";
        }
        return switch (currentStatus) {
            case UP -> "Up";
            case DOWN -> "Down";
            case UNKNOWN -> "Not checked";
        };
    }

    /** CSS modifier used in the UI. */
    public String getStatusClass() {
        if (!enabled) {
            return "off";
        }
        return switch (currentStatus) {
            case UP -> "up";
            case DOWN -> "down";
            case UNKNOWN -> "unknown";
        };
    }
}
