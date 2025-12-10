package com.example.payment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {

    @Query("select s from SubscriptionEntity s where s.status = 'ACTIVE' and s.nextBillingAt <= :now")
    List<SubscriptionEntity> findDueSubscriptions(@Param("now") Instant now);

    List<SubscriptionEntity> findByNextBillingAtBefore(Instant cutoff);
}
