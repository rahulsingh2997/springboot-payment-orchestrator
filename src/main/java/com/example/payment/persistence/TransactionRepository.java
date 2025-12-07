package com.example.payment.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends CrudRepository<TransactionEntity, String> {
	java.util.List<TransactionEntity> findByOrderIdAndTypeOrderByCreatedAtDesc(String orderId, com.example.payment.persistence.enums.TransactionType type);
	java.util.List<TransactionEntity> findByOrderIdOrderByCreatedAtDesc(String orderId);
}
