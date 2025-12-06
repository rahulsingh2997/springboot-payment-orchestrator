package com.example.payment.security;

import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    // Placeholder - implement JWT signing/validation in Phase 2
    public boolean validateToken(String token) {
        return token != null && !token.isEmpty();
    }
}
