package com.example.payment.workflow.queue;

public class MessagePayload {
    private String eventType;
    private String json;

    public MessagePayload(String eventType, String json) {
        this.eventType = eventType;
        this.json = json;
    }

    public String getEventType() { return eventType; }
    public String getJson() { return json; }
}
