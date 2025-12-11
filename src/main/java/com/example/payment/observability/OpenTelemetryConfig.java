package com.example.payment.observability;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    @Bean
    public OpenTelemetry openTelemetry() {
        LoggingSpanExporter loggingExporter = new LoggingSpanExporter();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(loggingExporter))
                .build();
        OpenTelemetrySdk otel = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        try {
            GlobalOpenTelemetry.set(otel);
        } catch (IllegalStateException ex) {
            // Global OpenTelemetry already set by another component; ignore to avoid startup failure
        }
        return otel;
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("com.example.payment", "1.0");
    }
}
