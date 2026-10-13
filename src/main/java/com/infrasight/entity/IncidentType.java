package com.infrasight.entity;

public enum IncidentType {
    DOWN("Endpoint down"), SLOW("Slow response");

    private final String label;

    IncidentType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
