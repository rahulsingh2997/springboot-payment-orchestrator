package com.example.payment.gateway.authorize;

import com.example.payment.gateway.mapper.AuthorizeNetRequest;
import com.example.payment.gateway.mapper.AuthorizeNetResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class NoOpAuthorizeNetConfig {

    @Bean
    public AuthorizeNetGateway authorizeNetGateway() {
        return new AuthorizeNetGateway() {
            @Override
            public AuthorizeNetResponse authorize(AuthorizeNetRequest request) {
                AuthorizeNetResponse r = new AuthorizeNetResponse();
                r.setSuccess(true);
                r.setTransactionId("noop-auth-" + UUID.randomUUID());
                r.setMessage("noop authorize");
                return r;
            }

            @Override
            public AuthorizeNetResponse capture(String transactionId) {
                AuthorizeNetResponse r = new AuthorizeNetResponse();
                r.setSuccess(true);
                r.setTransactionId(transactionId != null ? transactionId : ("noop-capture-" + UUID.randomUUID()));
                r.setMessage("noop capture");
                return r;
            }

            @Override
            public AuthorizeNetResponse voidTransaction(String transactionId) {
                AuthorizeNetResponse r = new AuthorizeNetResponse();
                r.setSuccess(true);
                r.setTransactionId(transactionId != null ? transactionId : ("noop-void-" + UUID.randomUUID()));
                r.setMessage("noop void");
                return r;
            }

            @Override
            public AuthorizeNetResponse refund(String transactionId, long amountCents) {
                AuthorizeNetResponse r = new AuthorizeNetResponse();
                r.setSuccess(true);
                r.setTransactionId(transactionId != null ? transactionId : ("noop-refund-" + UUID.randomUUID()));
                r.setMessage("noop refund: " + amountCents);
                return r;
            }

            @Override
            public AuthorizeNetResponse createSubscription(AuthorizeNetRequest request) {
                AuthorizeNetResponse r = new AuthorizeNetResponse();
                r.setSuccess(true);
                r.setTransactionId("noop-sub-" + UUID.randomUUID());
                r.setMessage("noop create subscription");
                return r;
            }

            @Override
            public AuthorizeNetResponse cancelSubscription(String subscriptionId) {
                AuthorizeNetResponse r = new AuthorizeNetResponse();
                r.setSuccess(true);
                r.setTransactionId(subscriptionId != null ? subscriptionId : ("noop-cancel-" + UUID.randomUUID()));
                r.setMessage("noop cancel subscription");
                return r;
            }
        };
    }
}
