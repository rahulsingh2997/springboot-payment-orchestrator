package com.example.payment.diagnostics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StartupFailureListener implements ApplicationListener<ApplicationFailedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupFailureListener.class);

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        log.error("Application failed to start. Capturing exception:", event.getException());
    }
}
