package com.infrasight.dto;

import com.infrasight.entity.MonitoredEndpoint;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EndpointForm {

    @NotBlank(message = "Enter a name.")
    @Size(max = 100, message = "Use 100 characters or fewer.")
    private String name;

    @NotBlank(message = "Enter a URL.")
    @Size(max = 2000, message = "Use 2000 characters or fewer.")
    private String url;

    @NotNull(message = "Enter a check interval.")
    @Min(value = 30, message = "The interval must be at least 30 seconds.")
    @Max(value = 86400, message = "The interval must be 86400 seconds (24 hours) or less.")
    private Integer intervalSeconds = 60;

    @NotNull(message = "Enter a timeout.")
    @Min(value = 1, message = "The timeout must be at least 1 second.")
    @Max(value = 30, message = "The timeout must be 30 seconds or less.")
    private Integer timeoutSeconds = 10;

    @Min(value = 100, message = "Enter a valid HTTP status code (100-599).")
    @Max(value = 599, message = "Enter a valid HTTP status code (100-599).")
    private Integer expectedStatus;

    @NotNull(message = "Enter a failure threshold.")
    @Min(value = 1, message = "The threshold must be at least 1.")
    @Max(value = 20, message = "The threshold must be 20 or less.")
    private Integer failureThreshold = 3;

    @Min(value = 100, message = "The slow-response limit must be at least 100 ms.")
    @Max(value = 60000, message = "The slow-response limit must be 60000 ms or less.")
    private Integer slowThresholdMs;

    private boolean enabled = true;

    public static EndpointForm from(MonitoredEndpoint e) {
        EndpointForm f = new EndpointForm();
        f.name = e.getName();
        f.url = e.getUrl();
        f.intervalSeconds = e.getIntervalSeconds();
        f.timeoutSeconds = e.getTimeoutSeconds();
        f.expectedStatus = e.getExpectedStatus();
        f.failureThreshold = e.getFailureThreshold();
        f.slowThresholdMs = e.getSlowThresholdMs();
        f.enabled = e.isEnabled();
        return f;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Integer getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Integer intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public Integer getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(Integer expectedStatus) { this.expectedStatus = expectedStatus; }
    public Integer getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(Integer failureThreshold) { this.failureThreshold = failureThreshold; }
    public Integer getSlowThresholdMs() { return slowThresholdMs; }
    public void setSlowThresholdMs(Integer slowThresholdMs) { this.slowThresholdMs = slowThresholdMs; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
