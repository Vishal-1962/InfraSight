package com.infrasight.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infrasight.dto.CheckSummary;
import com.infrasight.entity.CheckResult;
import com.infrasight.repository.CheckResultRepository;

@Service
public class HistoryService {

    public static final int PAGE_SIZE = 25;
    private static final LocalDateTime EARLIEST = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime LATEST = LocalDateTime.of(2999, 1, 1, 0, 0);

    private final CheckResultRepository results;

    public HistoryService(CheckResultRepository results) {
        this.results = results;
    }

    /** Maps the result filter of the UI to the repository mode: 0 all, 1 successful, 2 failed. */
    public static int mode(String result) {
        if ("success".equals(result)) {
            return 1;
        }
        if ("failed".equals(result)) {
            return 2;
        }
        return 0;
    }

    @Transactional(readOnly = true)
    public Page<CheckResult> search(LocalDate from, LocalDate to, long endpointId, int mode, Pageable pageable) {
        return results.search(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), endpointId, mode, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CheckResult> forEndpoint(long endpointId, int page, int size) {
        return results.search(EARLIEST, LATEST, endpointId, 0, PageRequest.of(Math.max(page, 0), size));
    }

    @Transactional(readOnly = true)
    public CheckSummary summary(LocalDateTime from, LocalDateTime to, long endpointId) {
        return StatsMapper.summary(results.summary(from, to, endpointId));
    }

    @Transactional(readOnly = true)
    public CheckSummary summary(LocalDate from, LocalDate to, long endpointId) {
        return summary(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), endpointId);
    }
}
