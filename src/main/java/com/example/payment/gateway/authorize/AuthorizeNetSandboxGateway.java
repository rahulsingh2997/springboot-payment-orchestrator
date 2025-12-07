package com.example.payment.gateway.authorize;

import com.example.payment.gateway.mapper.AuthorizeNetRequest;
import com.example.payment.gateway.mapper.AuthorizeNetResponse;
import net.authorize.Environment;
import net.authorize.api.contract.v1.*;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.base.ApiOperationBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class AuthorizeNetSandboxGateway implements AuthorizeNetGateway {

    @Value("${authorize.net.api.login:}")
    private String apiLogin;

    @Value("${authorize.net.transaction.key:}")
    private String transactionKey;

    private boolean hasCredentials() {
        return apiLogin != null && !apiLogin.isEmpty() && transactionKey != null && !transactionKey.isEmpty();
    }

    private void initMerchantAuth() {
        MerchantAuthenticationType merchantAuthenticationType = new MerchantAuthenticationType();
        merchantAuthenticationType.setName(apiLogin);
        merchantAuthenticationType.setTransactionKey(transactionKey);
        ApiOperationBase.setEnvironment(Environment.SANDBOX);
        ApiOperationBase.setMerchantAuthentication(merchantAuthenticationType);
    }

    @Override
    public AuthorizeNetResponse authorize(AuthorizeNetRequest request) {
        if (!hasCredentials()) {
            AuthorizeNetResponse r = new AuthorizeNetResponse();
            r.setSuccess(true);
            r.setTransactionId("noop-" + UUID.randomUUID().toString());
            r.setMessage("No credentials provided; returning noop success");
            return r;
        }

        try {
            initMerchantAuth();

            TransactionRequestType txnRequest = new TransactionRequestType();
            txnRequest.setTransactionType(TransactionTypeEnum.AUTH_ONLY_TRANSACTION.value());
            BigDecimal amt = new BigDecimal(request.getAmount());
            txnRequest.setAmount(amt);

            // For sandbox/demo we use a test card. In production, payment tokenization is required.
            CreditCardType creditCard = new CreditCardType();
            creditCard.setCardNumber("4111111111111111");
            creditCard.setExpirationDate("2038-12");
            PaymentType paymentType = new PaymentType();
            paymentType.setCreditCard(creditCard);
            txnRequest.setPayment(paymentType);

            CreateTransactionRequest apiRequest = new CreateTransactionRequest();
            apiRequest.setTransactionRequest(txnRequest);

            CreateTransactionController controller = new CreateTransactionController(apiRequest);
            controller.execute();
            CreateTransactionResponse response = controller.getApiResponse();

            AuthorizeNetResponse out = new AuthorizeNetResponse();
            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                TransactionResponse result = response.getTransactionResponse();
                if (result != null && result.getResponseCode() != null) {
                    out.setSuccess("1".equals(result.getResponseCode()) || "Ok".equalsIgnoreCase(result.getResponseCode()));
                    out.setTransactionId(result.getTransId());
                    out.setMessage(result.getMessages() != null && !result.getMessages().getMessage().isEmpty() ? result.getMessages().getMessage().get(0).getDescription() : "");
                } else {
                    out.setSuccess(false);
                    out.setMessage("No transaction response");
                }
            } else {
                out.setSuccess(false);
                out.setMessage(response != null && response.getMessages() != null && !response.getMessages().getMessage().isEmpty() ? response.getMessages().getMessage().get(0).getText() : "AuthorizeNet error");
            }
            return out;
        } catch (Exception ex) {
            AuthorizeNetResponse r = new AuthorizeNetResponse();
            r.setSuccess(false);
            r.setMessage("Exception: " + ex.getMessage());
            return r;
        }
    }

    @Override
    public AuthorizeNetResponse capture(String transactionId) {
        if (!hasCredentials()) {
            AuthorizeNetResponse r = new AuthorizeNetResponse();
            r.setSuccess(true);
            r.setTransactionId(transactionId);
            r.setMessage("No credentials provided; noop capture");
            return r;
        }

        try {
            initMerchantAuth();

            TransactionRequestType txnRequest = new TransactionRequestType();
            txnRequest.setTransactionType(TransactionTypeEnum.PRIOR_AUTH_CAPTURE_TRANSACTION.value());
            txnRequest.setRefTransId(transactionId);

            CreateTransactionRequest apiRequest = new CreateTransactionRequest();
            apiRequest.setTransactionRequest(txnRequest);

            CreateTransactionController controller = new CreateTransactionController(apiRequest);
            controller.execute();
            CreateTransactionResponse response = controller.getApiResponse();

            AuthorizeNetResponse out = new AuthorizeNetResponse();
            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                TransactionResponse result = response.getTransactionResponse();
                out.setSuccess(result != null && ("1".equals(result.getResponseCode()) || "Ok".equalsIgnoreCase(result.getResponseCode())));
                out.setTransactionId(result != null ? result.getTransId() : transactionId);
                out.setMessage(result != null && result.getMessages() != null && !result.getMessages().getMessage().isEmpty() ? result.getMessages().getMessage().get(0).getDescription() : "");
            } else {
                out.setSuccess(false);
                out.setMessage(response != null && response.getMessages() != null && !response.getMessages().getMessage().isEmpty() ? response.getMessages().getMessage().get(0).getText() : "AuthorizeNet error");
            }
            return out;
        } catch (Exception ex) {
            AuthorizeNetResponse r = new AuthorizeNetResponse();
            r.setSuccess(false);
            r.setMessage("Exception: " + ex.getMessage());
            return r;
        }
    }

    @Override
    public AuthorizeNetResponse voidTransaction(String transactionId) {
        AuthorizeNetResponse r = new AuthorizeNetResponse();
        r.setSuccess(false);
        r.setMessage("voidTransaction not implemented in sandbox gateway");
        return r;
    }

    @Override
    public AuthorizeNetResponse refund(String transactionId, long amountCents) {
        AuthorizeNetResponse r = new AuthorizeNetResponse();
        r.setSuccess(false);
        r.setMessage("refund not implemented in sandbox gateway");
        return r;
    }

    @Override
    public AuthorizeNetResponse createSubscription(AuthorizeNetRequest request) {
        AuthorizeNetResponse r = new AuthorizeNetResponse();
        r.setSuccess(false);
        r.setMessage("createSubscription not implemented in sandbox gateway");
        return r;
    }

    @Override
    public AuthorizeNetResponse cancelSubscription(String subscriptionId) {
        AuthorizeNetResponse r = new AuthorizeNetResponse();
        r.setSuccess(false);
        r.setMessage("cancelSubscription not implemented in sandbox gateway");
        return r;
    }
}
