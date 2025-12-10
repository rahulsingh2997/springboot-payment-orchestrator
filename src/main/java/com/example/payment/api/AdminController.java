package com.example.payment.api;

import com.example.payment.jobs.ReconciliationJob;
import com.example.payment.persistence.SubscriptionEntity;
import com.example.payment.persistence.SubscriptionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final SubscriptionRepository subscriptionRepository;
    private final ReconciliationJob reconciliationJob;

    public AdminController(SubscriptionRepository subscriptionRepository, ReconciliationJob reconciliationJob) {
        this.subscriptionRepository = subscriptionRepository;
        this.reconciliationJob = reconciliationJob;
    }

    @PostMapping("/subscriptions/{id}/make-due")
    public ResponseEntity<?> makeDue(@PathVariable String id) {
        Optional<SubscriptionEntity> sOpt = subscriptionRepository.findById(id);
        if (!sOpt.isPresent()) return ResponseEntity.notFound().build();
        SubscriptionEntity s = sOpt.get();
        s.setNextBillingAt(Instant.now().minusSeconds(60));
        subscriptionRepository.save(s);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reconcile")
    public ResponseEntity<?> triggerReconcile() {
        reconciliationJob.run();
        return ResponseEntity.ok().build();
    }
}
