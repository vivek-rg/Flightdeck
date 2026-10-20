package io.blackbox.backend.repository;

import io.blackbox.backend.model.IncidentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentEventRepository extends JpaRepository<IncidentEventEntity, Long> {
    List<IncidentEventEntity> findByIncidentIncidentIdOrderByTimestampAsc(String incidentId);
    List<IncidentEventEntity> findByTraceId(String traceId);
}
