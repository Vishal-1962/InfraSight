package com.infrasight.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.infrasight.entity.CheckResult;

/**
 * Filter parameters use sentinel values instead of nulls:
 * endpointId 0 = all endpoints; mode 0 = all results, 1 = successful only, 2 = failed only.
 */
public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    @Query(value = "select c from CheckResult c join fetch c.endpoint e "
            + "where c.checkedAt >= :from and c.checkedAt < :to "
            + "and (:endpointId = 0L or e.id = :endpointId) "
            + "and (:mode = 0 or (:mode = 1 and c.success = true) or (:mode = 2 and c.success = false)) "
            + "order by c.checkedAt desc",
            countQuery = "select count(c) from CheckResult c "
            + "where c.checkedAt >= :from and c.checkedAt < :to "
            + "and (:endpointId = 0L or c.endpoint.id = :endpointId) "
            + "and (:mode = 0 or (:mode = 1 and c.success = true) or (:mode = 2 and c.success = false))")
    Page<CheckResult> search(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                             @Param("endpointId") long endpointId, @Param("mode") int mode, Pageable pageable);

    @Query("select c from CheckResult c join fetch c.endpoint order by c.checkedAt desc")
    List<CheckResult> recent(Pageable pageable);
    @Query("select c from CheckResult c join fetch c.endpoint e " +
            "where e.id = :endpointId order by c.checkedAt desc")

    List<CheckResult> recentForEndpoint(
            @Param("endpointId") Long endpointId,
            Pageable pageable);

    /** One row: total, successful, average response ms, maximum response ms. */
    @Query("select count(c), sum(case when c.success = true then 1 else 0 end), avg(c.responseTimeMs), max(c.responseTimeMs) "
            + "from CheckResult c where c.checkedAt >= :from and c.checkedAt < :to "
            + "and (:endpointId = 0L or c.endpoint.id = :endpointId)")
    List<Object[]> summary(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                           @Param("endpointId") long endpointId);

    /** Hourly buckets: year, month, day, hour, checks, successful, sum of response ms, count of response ms. */
    @Query("select year(c.checkedAt), month(c.checkedAt), day(c.checkedAt), hour(c.checkedAt), "
            + "count(c), sum(case when c.success = true then 1 else 0 end), sum(c.responseTimeMs), count(c.responseTimeMs) "
            + "from CheckResult c where c.checkedAt >= :from and c.checkedAt < :to "
            + "and (:endpointId = 0L or c.endpoint.id = :endpointId) "
            + "group by year(c.checkedAt), month(c.checkedAt), day(c.checkedAt), hour(c.checkedAt) "
            + "order by year(c.checkedAt), month(c.checkedAt), day(c.checkedAt), hour(c.checkedAt)")
    List<Object[]> hourlyBuckets(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                 @Param("endpointId") long endpointId);

    /** Per endpoint: id, name, url, checks, successful, average ms, maximum ms. */
    @Query("select e.id, e.name, e.url, count(c), sum(case when c.success = true then 1 else 0 end), "
            + "avg(c.responseTimeMs), max(c.responseTimeMs) "
            + "from CheckResult c join c.endpoint e where c.checkedAt >= :from and c.checkedAt < :to "
            + "group by e.id, e.name, e.url order by e.name")
    List<Object[]> reportByEndpoint(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Modifying
    @Query("delete from CheckResult c where c.checkedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("delete from CheckResult c where c.endpoint.id = :endpointId")
    int deleteByEndpointId(@Param("endpointId") Long endpointId);
}
