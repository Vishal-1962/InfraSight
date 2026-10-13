package com.infrasight.dto;

/** Result of one HTTP check, before it is stored. */
public record CheckOutcome(boolean success, Integer statusCode, Long responseTimeMs, String error) {
}
