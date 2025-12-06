package com.example.payment.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookEventRepository extends CrudRepository<WebhookEventEntity, String> {

}
