package com.infrasight.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.infrasight.entity.EndpointStatus;
import com.infrasight.entity.MonitoredEndpoint;

public interface EndpointRepository extends JpaRepository<MonitoredEndpoint, Long> {

    List<MonitoredEndpoint> findAllByOrderByNameAsc();

    List<MonitoredEndpoint> findByEnabledTrue();

    long countByEnabledTrue();

    long countByEnabledTrueAndCurrentStatus(EndpointStatus status);
}
