package io.blackbox.backend.repository;

import io.blackbox.backend.model.SpanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpanRepository extends JpaRepository<SpanEntity, String> {

    List<SpanEntity> findByTraceIdOrderByStartTimeAsc(String traceId);

    List<SpanEntity> findByServiceNameAndStartTimeBetween(String serviceName, Long start, Long end);

    @Query("SELECT s FROM SpanEntity s WHERE s.startTime >= :since ORDER BY s.startTime DESC")
    List<SpanEntity> findRecentSpans(Long since);

    @Query("SELECT s FROM SpanEntity s WHERE s.isError = true AND s.startTime >= :since ORDER BY s.startTime DESC")
    List<SpanEntity> findRecentErrors(Long since);

    long countByStartTimeAfter(Long since);

    long countByIsErrorTrueAndStartTimeAfter(Long since);
}
