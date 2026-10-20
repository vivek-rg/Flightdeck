package io.blackbox.sample.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class OrderDto implements Serializable {
    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private String status;
    private List<OrderItemDto> items;
    private String traceId;
    private String failureReason;
    private Long createdAt;

    public OrderDto() {}

    public OrderDto(String orderId, String customerId, BigDecimal totalAmount, String status, List<OrderItemDto> items) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.items = items;
        this.createdAt = System.currentTimeMillis();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
