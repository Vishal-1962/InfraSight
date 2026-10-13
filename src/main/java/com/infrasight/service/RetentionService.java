package com.infrasight.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infrasight.config.InfraSightProperties;
import com.infrasight.repository.CheckResultRepository;

/** Deletes old check results so the history table cannot grow without limit. Incidents are kept. */
@Service
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    private final CheckResultRepository results;
    private final InfraSightProperties properties;

    public RetentionService(CheckResultRepository results, InfraSightProperties properties) {
        this.results = results;
        this.properties = properties;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 6 * 60 * 60 * 1000L)
    public void cleanUp() {
        int days = properties.getRetentionDays();
        if (days <= 0) {
            return;
        }
        try {
            int deleted = purge(LocalDateTime.now().minusDays(days));
            if (deleted > 0) {
                log.info("Retention: deleted {} check results older than {} days", deleted, days);
            }
        } catch (RuntimeException ex) {
            log.warn("Retention cleanup failed: {}", ex.toString());
        }
    }

    @Transactional
    public int purge(LocalDateTime cutoff) {
        return results.deleteOlderThan(cutoff);
    }
}
