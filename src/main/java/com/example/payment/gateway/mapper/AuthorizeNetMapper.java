package com.example.payment.gateway.mapper;

/**
 * Mapper stub for Authorize.Net gateway conversions.
 * No business logic; only stub methods for mapping.
 */
public class AuthorizeNetMapper {

    public AuthorizeNetRequest toAuthorizeNetRequest(Object source) {
        // stub: convert domain object to gateway request
        return new AuthorizeNetRequest();
    }

    public Object fromAuthorizeNetResponse(AuthorizeNetResponse response) {
        // stub: convert gateway response to domain object
        return null;
    }
}
