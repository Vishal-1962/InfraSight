package com.infrasight.service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.infrasight.dto.CheckOutcome;
import com.infrasight.entity.CheckResult;
import com.infrasight.entity.MonitoredEndpoint;
import com.infrasight.repository.EndpointRepository;

import jakarta.annotation.PreDestroy;

@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final EndpointRepository endpoints;
    private final HttpChecker checker;
    private final CheckRecorder recorder;

    private final Set<Long> running = ConcurrentHashMap.newKeySet();
    private final AtomicInteger threadCounter = new AtomicInteger();
    private final ExecutorService executor = Executors.newFixedThreadPool(6, runnable -> {
        Thread thread = new Thread(runnable, "infrasight-check-" + threadCounter.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    public HealthCheckService(EndpointRepository endpoints, HttpChecker checker, CheckRecorder recorder) {
        this.endpoints = endpoints;
        this.checker = checker;
        this.recorder = recorder;
    }

    /**
     * Runs a check immediately on the calling thread.
     * Returns null if a check for this endpoint is already in progress.
     */
    public CheckResult runNow(Long endpointId) {
        MonitoredEndpoint endpoint = endpoints.findById(endpointId)
                .orElseThrow(() -> new NotFoundException("Endpoint not found."));
        if (!running.add(endpointId)) {
            return null;
        }
        try {
            CheckOutcome outcome = checker.check(endpoint);
            return recorder.record(endpointId, outcome);
        } finally {
            running.remove(endpointId);
        }
    }

    /** Looks for enabled endpoints whose interval has elapsed and checks them in the background. */
    @Scheduled(initialDelay = 10_000, fixedDelay = 10_000)
    public void runDueChecks() {
        LocalDateTime now = LocalDateTime.now();
        try {
            for (MonitoredEndpoint endpoint : endpoints.findByEnabledTrue()) {
                LocalDateTime last = endpoint.getLastCheckedAt();
                boolean due = last == null || !last.plusSeconds(endpoint.getIntervalSeconds()).isAfter(now);
                if (!due || !running.add(endpoint.getId())) {
                    continue;
                }
                executor.submit(() -> {
                    try {
                        recorder.record(endpoint.getId(), checker.check(endpoint));
                    } catch (RuntimeException ex) {
                        log.warn("Scheduled check failed for endpoint {}: {}", endpoint.getId(), ex.toString());
                    } finally {
                        running.remove(endpoint.getId());
                    }
                });
            }
        } catch (RuntimeException ex) {
            log.warn("Could not schedule checks: {}", ex.toString());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
