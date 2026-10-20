package io.blackbox.sample.common.failure;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FailureInjectionRegistry {

    private final AtomicLong redisLatencyMs = new AtomicLong(0);
    private final AtomicBoolean redisUnavailable = new AtomicBoolean(false);

    private final AtomicLong paymentDelayMs = new AtomicLong(0);
    private final AtomicBoolean paymentTimeout = new AtomicBoolean(false);
    private final AtomicInteger paymentFailuresCount = new AtomicInteger(0);

    private final AtomicLong kafkaConsumerDelayMs = new AtomicLong(0);
    private final AtomicBoolean kafkaDuplicateEvent = new AtomicBoolean(false);

    private final AtomicBoolean databaseUnavailable = new AtomicBoolean(false);
    private final AtomicBoolean serviceUnavailable = new AtomicBoolean(false);

    public void setRedisLatency(long ms) {
        this.redisLatencyMs.set(ms);
    }

    public long getRedisLatency() {
        return redisLatencyMs.get();
    }

    public void setRedisUnavailable(boolean unavailable) {
        this.redisUnavailable.set(unavailable);
    }

    public boolean isRedisUnavailable() {
        return redisUnavailable.get();
    }

    public void setPaymentDelay(long ms) {
        this.paymentDelayMs.set(ms);
    }

    public long getPaymentDelay() {
        return paymentDelayMs.get();
    }

    public void setPaymentTimeout(boolean timeout) {
        this.paymentTimeout.set(timeout);
    }

    public boolean isPaymentTimeout() {
        return paymentTimeout.get();
    }

    public void setPaymentFailuresCount(int count) {
        this.paymentFailuresCount.set(count);
    }

    public boolean decrementAndCheckPaymentFailure() {
        return paymentFailuresCount.getAndUpdate(c -> c > 0 ? c - 1 : 0) > 0;
    }

    public void setKafkaConsumerDelay(long ms) {
        this.kafkaConsumerDelayMs.set(ms);
    }

    public long getKafkaConsumerDelay() {
        return kafkaConsumerDelayMs.get();
    }

    public void setKafkaDuplicateEvent(boolean duplicate) {
        this.kafkaDuplicateEvent.set(duplicate);
    }

    public boolean isKafkaDuplicateEvent() {
        return kafkaDuplicateEvent.get();
    }

    public void setDatabaseUnavailable(boolean unavailable) {
        this.databaseUnavailable.set(unavailable);
    }

    public boolean isDatabaseUnavailable() {
        return databaseUnavailable.get();
    }

    public void setServiceUnavailable(boolean unavailable) {
        this.serviceUnavailable.set(unavailable);
    }

    public boolean isServiceUnavailable() {
        return serviceUnavailable.get();
    }

    public void clearAll() {
        redisLatencyMs.set(0);
        redisUnavailable.set(false);
        paymentDelayMs.set(0);
        paymentTimeout.set(false);
        paymentFailuresCount.set(0);
        kafkaConsumerDelayMs.set(0);
        kafkaDuplicateEvent.set(false);
        databaseUnavailable.set(false);
        serviceUnavailable.set(false);
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("redisLatencyMs", redisLatencyMs.get());
        status.put("redisUnavailable", redisUnavailable.get());
        status.put("paymentDelayMs", paymentDelayMs.get());
        status.put("paymentTimeout", paymentTimeout.get());
        status.put("paymentFailuresRemaining", paymentFailuresCount.get());
        status.put("kafkaConsumerDelayMs", kafkaConsumerDelayMs.get());
        status.put("kafkaDuplicateEvent", kafkaDuplicateEvent.get());
        status.put("databaseUnavailable", databaseUnavailable.get());
        status.put("serviceUnavailable", serviceUnavailable.get());
        return status;
    }
}
