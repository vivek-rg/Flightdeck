package io.blackbox.sample.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class PaymentResponseDto implements Serializable {
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private long latencyMs;
    private String traceId;
    private String error;

    public PaymentResponseDto() {}

    public PaymentResponseDto(String paymentId, String orderId, BigDecimal amount, String status, long latencyMs) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.latencyMs = latencyMs;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
