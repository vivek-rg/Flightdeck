package io.blackbox.backend.repository;

import io.blackbox.backend.model.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, String> {

    @Query("SELECT i FROM IncidentEntity i ORDER BY i.startTime DESC")
    List<IncidentEntity> findAllOrderByStartTimeDesc();

    List<IncidentEntity> findByStatus(String status);

    long countByStatus(String status);

    @Query("SELECT i FROM IncidentEntity i WHERE i.status = 'ACTIVE' ORDER BY i.startTime DESC")
    List<IncidentEntity> findActiveIncidents();
}
