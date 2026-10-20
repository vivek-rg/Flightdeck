package io.blackbox.sample.order.repository;

import io.blackbox.sample.order.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    List<OrderEntity> findByTraceId(String traceId);
    List<OrderEntity> findByCustomerId(String customerId);
}
