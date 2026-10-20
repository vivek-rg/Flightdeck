package io.blackbox.backend.repository;

import io.blackbox.backend.model.TraceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TraceRepository extends JpaRepository<TraceEntity, String> {

    @Query("SELECT t FROM TraceEntity t ORDER BY t.startTime DESC")
    List<TraceEntity> findRecentTraces();

    List<TraceEntity> findByHasErrorTrueOrderByStartTimeDesc();

    long countByStartTimeAfter(Long since);
}
