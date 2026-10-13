package com.infrasight.dto;

/** One heartbeat bar: state is "ok", "fail" or "none" (no check yet). */
public record Beat(String state, String title) {
}
