package com.infrasight.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infrasight.dto.EndpointForm;
import com.infrasight.entity.EndpointStatus;
import com.infrasight.entity.MonitoredEndpoint;
import com.infrasight.repository.CheckResultRepository;
import com.infrasight.repository.EndpointRepository;
import com.infrasight.repository.IncidentRepository;

@Service
public class EndpointService {

    private final EndpointRepository endpoints;
    private final CheckResultRepository results;
    private final IncidentRepository incidents;
    private final UrlValidator urlValidator;

    public EndpointService(EndpointRepository endpoints, CheckResultRepository results,
                           IncidentRepository incidents, UrlValidator urlValidator) {
        this.endpoints = endpoints;
        this.results = results;
        this.incidents = incidents;
        this.urlValidator = urlValidator;
    }

    @Transactional(readOnly = true)
    public List<MonitoredEndpoint> findAll() {
        return endpoints.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public MonitoredEndpoint get(Long id) {
        return endpoints.findById(id).orElseThrow(() -> new NotFoundException("Endpoint not found."));
    }

    /** Throws UnsafeUrlException if the URL is invalid or points to a blocked destination. */
    @Transactional
    public MonitoredEndpoint create(EndpointForm form) {
        String url = urlValidator.validate(form.getUrl()).toString();
        MonitoredEndpoint endpoint = new MonitoredEndpoint();
        apply(endpoint, form, url);
        return endpoints.save(endpoint);
    }

    /** Throws UnsafeUrlException if the URL is invalid or points to a blocked destination. */
    @Transactional
    public MonitoredEndpoint update(Long id, EndpointForm form) {
        MonitoredEndpoint endpoint = get(id);
        String url = urlValidator.validate(form.getUrl()).toString();
        boolean urlChanged = !url.equals(endpoint.getUrl());
        apply(endpoint, form, url);
        if (urlChanged) {
            endpoint.setCurrentStatus(EndpointStatus.UNKNOWN);
            endpoint.setConsecutiveFailures(0);
            endpoint.setConsecutiveSlow(0);
            endpoint.setDownAlertRaised(false);
            endpoint.setSlowAlertRaised(false);
            endpoint.setLastCheckedAt(null);
            endpoint.setLastResponseTimeMs(null);
            endpoint.setLastStatusCode(null);
        }
        return endpoints.save(endpoint);
    }

    @Transactional
    public MonitoredEndpoint toggle(Long id) {
        MonitoredEndpoint endpoint = get(id);
        endpoint.setEnabled(!endpoint.isEnabled());
        return endpoints.save(endpoint);
    }

    /** Deletes the endpoint together with its check history and incidents. */
    @Transactional
    public void delete(Long id) {
        MonitoredEndpoint endpoint = get(id);
        results.deleteByEndpointId(id);
        incidents.deleteByEndpointId(id);
        endpoints.delete(endpoint);
    }

    private void apply(MonitoredEndpoint endpoint, EndpointForm form, String url) {
        endpoint.setName(form.getName().trim());
        endpoint.setUrl(url);
        endpoint.setIntervalSeconds(form.getIntervalSeconds());
        endpoint.setTimeoutSeconds(form.getTimeoutSeconds());
        endpoint.setExpectedStatus(form.getExpectedStatus());
        endpoint.setFailureThreshold(form.getFailureThreshold());
        endpoint.setSlowThresholdMs(form.getSlowThresholdMs());
        endpoint.setEnabled(form.isEnabled());
    }
}
