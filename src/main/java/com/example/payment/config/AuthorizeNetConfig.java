package com.example.payment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Authorize.Net SDK types. Ensure `anet-java-sdk` is present in pom.xml.
import net.authorize.Environment;
import net.authorize.api.contract.v1.MerchantAuthenticationType;

@Configuration
public class AuthorizeNetConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeNetConfig.class);

    @Value("${authorize-net.api.login-id:}")
    private String apiLoginId;

    @Value("${authorize-net.api.transaction-key:}")
    private String transactionKey;

    @Value("${authorize-net.api.environment:SANDBOX}")
    private String environment;

    @Bean
    public MerchantAuthenticationType merchantAuthentication() {
        if (apiLoginId == null || apiLoginId.trim().isEmpty() || transactionKey == null || transactionKey.trim().isEmpty()) {
            log.warn("Authorize.Net credentials not configured (authorize-net.api.login-id / transaction-key). Skipping MerchantAuthentication bean creation.");
            return null;
        }

        MerchantAuthenticationType merchantAuthentication = new MerchantAuthenticationType();
        merchantAuthentication.setName(apiLoginId);
        merchantAuthentication.setTransactionKey(transactionKey);
        return merchantAuthentication;
    }

    @Bean
    public Environment authorizeNetEnvironment() {
        return "PRODUCTION".equalsIgnoreCase(environment) ? Environment.PRODUCTION : Environment.SANDBOX;
    }

    // Getters for use in services
    public String getApiLoginId() { return apiLoginId; }

    public String getTransactionKey() { return transactionKey; }

}
