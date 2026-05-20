package com.solarride.solarride.repository;

import com.solarride.solarride.domain.payment.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    boolean existsByTransactionReference(String transactionReference);

    Optional<Transaction> findByTransactionReference(String transactionReference);

    List<Transaction> findByJobIdOrderByCreatedAtDesc(UUID jobId);
}