package com.solarride.solarride.repository;

import com.solarride.solarride.domain.job.Quote;
import com.solarride.solarride.domain.job.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    List<Quote> findByJobIdAndDeletedAtIsNull(UUID jobId);

    Optional<Quote> findByIdAndDeletedAtIsNull(UUID id);

    long countByJobIdAndDeletedAtIsNull(UUID jobId);

    List<Quote> findByJobIdAndStatus(UUID jobId, QuoteStatus status);

    boolean existsByJobIdAndInstallerIdAndDeletedAtIsNull(UUID jobId, UUID installerId);

    @Query("SELECT q FROM Quote q WHERE q.status = 'REQUESTED' AND q.slaDeadlineAt < :now AND q.deletedAt IS NULL")
    List<Quote> findOverdueRequestedQuotes(@Param("now") Instant now);
}