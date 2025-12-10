package com.example.payment.workflow.queue;

public interface QueueConsumer {
    void consume(String topic, String payload);
}
