package com.example.payment.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.Filter;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceFilter implements Filter {

    private final Tracer tracer;

    public TraceFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String cid = MDC.get("correlationId");
        Span span = tracer.spanBuilder(req.getMethod() + " " + req.getRequestURI()).startSpan();
        try (Scope scope = span.makeCurrent()) {
            // put trace info into MDC for structured logs
            String traceId = span.getSpanContext().getTraceId();
            String spanId = span.getSpanContext().getSpanId();
            if (traceId != null && !traceId.isEmpty()) MDC.put("traceId", traceId);
            if (spanId != null && !spanId.isEmpty()) MDC.put("spanId", spanId);
            if (cid != null && !cid.isEmpty()) span.setAttribute("correlationId", cid);
            chain.doFilter(request, response);
        } finally {
            span.end();
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }
}
