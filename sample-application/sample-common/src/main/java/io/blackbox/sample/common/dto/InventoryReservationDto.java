package io.blackbox.sample.common.dto;

import java.io.Serializable;
import java.util.List;

public class InventoryReservationDto implements Serializable {
    private String orderId;
    private List<OrderItemDto> items;
    private String status;
    private String message;
    private String traceId;

    public InventoryReservationDto() {}

    public InventoryReservationDto(String orderId, List<OrderItemDto> items, String status, String message) {
        this.orderId = orderId;
        this.items = items;
        this.status = status;
        this.message = message;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
