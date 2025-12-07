package com.example.payment.gateway.authorize;

import com.example.payment.gateway.mapper.AuthorizeNetRequest;
import com.example.payment.gateway.mapper.AuthorizeNetResponse;

/**
 * Skeleton adapter for Authorize.Net gateway interactions.
 * Implementations will call the Authorize.Net SDK.
 */
public interface AuthorizeNetGateway {

    // Basic payment operations (stubs)
    AuthorizeNetResponse authorize(AuthorizeNetRequest request);

    AuthorizeNetResponse capture(String transactionId);

    AuthorizeNetResponse voidTransaction(String transactionId);

    AuthorizeNetResponse refund(String transactionId, long amountCents);

    // Subscription helpers (kept as previously added)
    AuthorizeNetResponse createSubscription(AuthorizeNetRequest request);

    AuthorizeNetResponse cancelSubscription(String subscriptionId);
}
