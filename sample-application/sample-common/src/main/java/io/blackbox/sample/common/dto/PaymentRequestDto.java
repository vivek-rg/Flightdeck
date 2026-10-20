package io.blackbox.sample.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class PaymentRequestDto implements Serializable {
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private String traceId;

    public PaymentRequestDto() {}

    public PaymentRequestDto(String orderId, String customerId, BigDecimal amount, String paymentMethod) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
