package com.example.payment.diagnostics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

@Component
public class ShutdownDiagnostics {
    private static final Logger log = LoggerFactory.getLogger(ShutdownDiagnostics.class);

    @PostConstruct
    public void init() {
        log.info("ShutdownDiagnostics initialized. Registering JVM shutdown hook to capture diagnostics.");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.warn("JVM shutdown hook invoked - dumping thread info and basic diagnostics");
                dumpThreads();
            } catch (Throwable t) {
                log.error("Error during shutdown diagnostics", t);
            }
        }));
    }

    @PreDestroy
    public void onPreDestroy() {
        log.warn("@PreDestroy called on ShutdownDiagnostics - application context is closing. Dumping threads and environment snapshot.");
        dumpThreads();
    }

    private void dumpThreads() {
        try {
            Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
            log.warn("Thread dump ({} threads):", all.size());
            for (Map.Entry<Thread, StackTraceElement[]> e : all.entrySet()) {
                Thread t = e.getKey();
                StackTraceElement[] st = e.getValue();
                StringBuilder sb = new StringBuilder();
                sb.append("Thread[name=").append(t.getName()).append(", id=").append(t.getId()).append("]\n");
                for (StackTraceElement s : st) {
                    sb.append("    at ").append(s.toString()).append("\n");
                }
                log.warn(sb.toString());
            }
        } catch (Throwable ex) {
            log.error("Failed to produce thread dump", ex);
        }
    }
}
