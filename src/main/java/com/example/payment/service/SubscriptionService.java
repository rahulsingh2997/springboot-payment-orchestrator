package com.example.payment.service;

import com.example.payment.persistence.SubscriptionEntity;

import java.util.Optional;

public interface SubscriptionService {
    SubscriptionEntity createSubscription(SubscriptionEntity req);
    Optional<SubscriptionEntity> getSubscription(String id);
    void renewDueSubscriptions();
    void renewSubscription(String id);
}
