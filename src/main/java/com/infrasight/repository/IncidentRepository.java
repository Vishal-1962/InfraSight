package com.infrasight.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.infrasight.entity.Incident;
import com.infrasight.entity.IncidentStatus;
import com.infrasight.entity.IncidentType;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query(value = "select i from Incident i join fetch i.endpoint e "
            + "where i.status in :statuses and (:endpointId = 0L or e.id = :endpointId) "
            + "order by i.openedAt desc",
            countQuery = "select count(i) from Incident i "
            + "where i.status in :statuses and (:endpointId = 0L or i.endpoint.id = :endpointId)")
    Page<Incident> search(@Param("statuses") Collection<IncidentStatus> statuses,
                          @Param("endpointId") long endpointId, Pageable pageable);

    @Query("select i from Incident i join fetch i.endpoint order by i.openedAt desc")
    List<Incident> recent(Pageable pageable);

    @Query("select i from Incident i join fetch i.endpoint where i.openedAt >= :from and i.openedAt < :to "
            + "order by i.openedAt desc")
    List<Incident> openedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    /** Rows: severity, status, count. */
    @Query("select i.severity, i.status, count(i) from Incident i "
            + "where i.openedAt >= :from and i.openedAt < :to group by i.severity, i.status")
    List<Object[]> countsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByStatusIn(Collection<IncidentStatus> statuses);

    List<Incident> findTop5ByEndpointIdOrderByOpenedAtDesc(Long endpointId);

    List<Incident> findByEndpointIdAndTypeAndStatusNot(Long endpointId, IncidentType type, IncidentStatus status);

    boolean existsByEndpointIdAndTypeAndStatusNot(Long endpointId, IncidentType type, IncidentStatus status);

    @Modifying
    @Query("delete from Incident i where i.endpoint.id = :endpointId")
    int deleteByEndpointId(@Param("endpointId") Long endpointId);
}
