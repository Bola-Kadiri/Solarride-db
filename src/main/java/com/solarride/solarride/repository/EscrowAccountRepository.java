package com.solarride.solarride.repository;

import com.solarride.solarride.domain.payment.EscrowAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EscrowAccountRepository extends JpaRepository<EscrowAccount, UUID> {

    Optional<EscrowAccount> findByJobId(UUID jobId);

    boolean existsByJobId(UUID jobId);
}