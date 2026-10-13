package com.infrasight.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.infrasight.entity.EndpointStatus;
import com.infrasight.repository.EndpointRepository;
import com.infrasight.service.IncidentService;

/** Small counters shown in the navigation on every page. */
@ControllerAdvice
public class GlobalModelAttributes {

    private final EndpointRepository endpoints;
    private final IncidentService incidents;

    public GlobalModelAttributes(EndpointRepository endpoints, IncidentService incidents) {
        this.endpoints = endpoints;
        this.incidents = incidents;
    }

    @ModelAttribute("navTotal")
    public long navTotal() {
        return endpoints.countByEnabledTrue();
    }

    @ModelAttribute("navDown")
    public long navDown() {
        return endpoints.countByEnabledTrueAndCurrentStatus(EndpointStatus.DOWN);
    }

    @ModelAttribute("navIncidents")
    public long navIncidents() {
        return incidents.activeCount();
    }
}
