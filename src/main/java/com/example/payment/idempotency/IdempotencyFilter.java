package com.example.payment.idempotency;

import com.example.payment.persistence.IdempotencyKeyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);

    private final IdempotencyService idempotencyService;

    public IdempotencyFilter(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // only protect API writes (POST/PUT/PATCH) under /api/v1/
        String method = request.getMethod();
        return !(path.startsWith("/api/v1/") && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String idempotencyKey = request.getHeader(IdempotencyInterceptor.IDEMPOTENCY_HEADER);
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        byte[] body = cachedRequest.getCachedBody();
        String hashInput = request.getMethod() + "|" + request.getRequestURI() + "|" + Arrays.toString(body);
        String requestHash = DigestUtils.md5DigestAsHex(hashInput.getBytes(StandardCharsets.UTF_8));

        // claim via service
        IdempotencyService.ClaimResult claim = idempotencyService.claim(idempotencyKey, requestHash);
        if (claim.getType() == IdempotencyService.ClaimResult.Type.CONFLICT) {
            log.warn("Idempotency key conflict for {}: incoming hash != stored", idempotencyKey);
            response.setStatus(HttpStatus.CONFLICT.value());
            response.getWriter().write("Idempotency key conflict");
            return;
        }
        if (claim.getType() == IdempotencyService.ClaimResult.Type.REPLAY) {
            IdempotencyKeyEntity existing = claim.getEntity();
            int status = existing.getResponseStatus() != null ? existing.getResponseStatus() : HttpStatus.OK.value();
            response.setStatus(status);
            // restore headers if present
            if (existing.getResponseHeaders() != null && !existing.getResponseHeaders().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String,String> headers = om.readValue(existing.getResponseHeaders(), java.util.Map.class);
                    headers.forEach(response::setHeader);
                } catch (Exception ex) {
                    log.warn("Failed to parse stored response headers", ex);
                }
            }
            response.setContentType("application/json");
            response.getWriter().write(existing.getResponseBody() != null ? existing.getResponseBody() : "{}");
            return;
        }

        // wrap response to capture body
        BufferingHttpServletResponseWrapper bufferingResponse = new BufferingHttpServletResponseWrapper(response);
        filterChain.doFilter(cachedRequest, bufferingResponse);

        byte[] respBytes = bufferingResponse.getCopy();
        String responseBody = new String(respBytes, StandardCharsets.UTF_8);

        // save full response snapshot (status + headers + body)
        try {
            int respStatus = bufferingResponse.getStatus();
            String headersJson = bufferingResponse.getHeadersJson();
            idempotencyService.saveResponse(idempotencyKey, respStatus, headersJson, responseBody);
        } catch (Exception ex) {
            log.error("Failed to save idempotency response", ex);
        }

        // copy buffered content to actual response
        bufferingResponse.copyBodyToResponse();
    }

    private static class BufferingHttpServletResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final ServletOutputStream outputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }

            @Override
            public void setWriteListener(javax.servlet.WriteListener listener) { }

            @Override
            public void write(int b) throws IOException { buffer.write(b); }
        };
        private java.io.PrintWriter writer;
        private int httpStatus = 200;
        private final java.util.Map<String, java.util.List<String>> headers = new java.util.HashMap<>();

        public BufferingHttpServletResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            super.setStatus(sc);
            this.httpStatus = sc;
        }

        @Override
        public void setHeader(String name, String value) {
            super.setHeader(name, value);
            java.util.List<String> list = new java.util.ArrayList<>();
            list.add(value);
            headers.put(name, list);
        }

        @Override
        public void addHeader(String name, String value) {
            super.addHeader(name, value);
            headers.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(value);
        }

        @Override
        public ServletOutputStream getOutputStream() { return outputStream; }

        @Override
        public java.io.PrintWriter getWriter() throws IOException {
            if (writer == null) {
                java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(buffer, getCharacterEncoding() != null ? getCharacterEncoding() : java.nio.charset.StandardCharsets.UTF_8.name());
                writer = new java.io.PrintWriter(osw, true);
            }
            return writer;
        }

        public byte[] getCopy() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            return buffer.toByteArray();
        }

        public void copyBodyToResponse() throws IOException {
            byte[] bytes = getCopy();
            javax.servlet.ServletOutputStream out = getResponse().getOutputStream();
            out.write(bytes);
            out.flush();
        }

        public int getStatus() { return httpStatus; }

        public String getHeadersJson() {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String,String> single = new java.util.HashMap<>();
                headers.forEach((k,v)-> single.put(k, String.join(",", v)));
                return om.writeValueAsString(single);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
