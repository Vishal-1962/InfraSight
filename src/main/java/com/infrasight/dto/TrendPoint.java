package com.infrasight.dto;

public record TrendPoint(String label, Double avgResponseMs, Double availability, long checks) {
}
