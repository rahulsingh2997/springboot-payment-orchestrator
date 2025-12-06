package com.example.payment.api;

import com.example.payment.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    public AuthController(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/dev/token")
    public ResponseEntity<TokenResponse> devToken(@RequestBody TokenRequest req) {
        List<String> roles = req.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = java.util.Arrays.asList("ROLE_USER");
        }
        String token = tokenProvider.generateToken(req.getUsername(), roles);
        TokenResponse resp = new TokenResponse(token, tokenProvider.getSubject(token));
        return ResponseEntity.ok(resp);
    }

    public static class TokenRequest {
        private String username;
        private List<String> roles;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

    public static class TokenResponse {
        private String token;
        private String subject;

        public TokenResponse() {}
        public TokenResponse(String token, String subject) { this.token = token; this.subject = subject; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
    }
}
