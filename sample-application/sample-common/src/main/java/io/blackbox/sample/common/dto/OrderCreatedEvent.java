package io.blackbox.sample.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class OrderCreatedEvent implements Serializable {
    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private String status;
    private List<OrderItemDto> items;
    private long timestamp;
    private String traceId;

    public OrderCreatedEvent() {}

    public OrderCreatedEvent(String orderId, String customerId, BigDecimal totalAmount, String status, List<OrderItemDto> items, String traceId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.items = items;
        this.timestamp = System.currentTimeMillis();
        this.traceId = traceId;
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

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
