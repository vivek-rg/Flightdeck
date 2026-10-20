package io.blackbox.backend.repository;

import io.blackbox.backend.model.AnomalyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEntity, Long> {

    @Query("SELECT a FROM AnomalyEntity a ORDER BY a.timestamp DESC")
    List<AnomalyEntity> findAllOrderByTimestampDesc();

    List<AnomalyEntity> findByServiceName(String serviceName);

    List<AnomalyEntity> findByIncidentId(String incidentId);

    @Query("SELECT a FROM AnomalyEntity a WHERE a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AnomalyEntity> findRecentAnomalies(Long since);
}
