package com.example.payment.common;

import com.example.payment.idempotency.IdempotencyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    private IdempotencyInterceptor idempotencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (idempotencyInterceptor != null) {
            registry.addInterceptor(idempotencyInterceptor).addPathPatterns("/api/**");
        }
    }
}
