package io.blackbox.backend.repository;

import io.blackbox.backend.model.ReplayRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplayRunRepository extends JpaRepository<ReplayRunEntity, String> {
    List<ReplayRunEntity> findByIncidentIdOrderByStartTimeDesc(String incidentId);
}
