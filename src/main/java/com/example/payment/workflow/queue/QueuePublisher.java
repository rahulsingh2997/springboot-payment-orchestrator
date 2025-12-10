package com.example.payment.workflow.queue;

public interface QueuePublisher {
    void publish(String topic, String payload);
}
