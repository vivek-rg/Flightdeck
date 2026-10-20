package io.blackbox.backend.repository;

import io.blackbox.backend.model.RootCauseCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RootCauseCandidateRepository extends JpaRepository<RootCauseCandidateEntity, Long> {
    List<RootCauseCandidateEntity> findByIncidentIdOrderByScoreDesc(String incidentId);
    void deleteByIncidentId(String incidentId);
}
