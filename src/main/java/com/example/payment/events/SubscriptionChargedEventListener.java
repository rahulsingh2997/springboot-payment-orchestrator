package com.example.payment.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionChargedEventListener {

    private final Logger log = LoggerFactory.getLogger(SubscriptionChargedEventListener.class);

    @EventListener
    public void handle(SubscriptionChargedEvent event) {
        String cid = MDC.get("correlationId");
        log.info("SubscriptionChargedEvent published subscriptionId={} amount={} correlationId={} eventSchemaVersion={} timestamp={}",
                event.getSubscriptionId(), event.getAmountCents(), cid, event.getEventSchemaVersion(), event.getTimestamp());
    }
}
