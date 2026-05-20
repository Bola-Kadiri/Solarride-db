package com.solarride.solarride.repository;

import com.solarride.solarride.domain.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByJobId(UUID jobId);

    boolean existsByJobId(UUID jobId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.installer.id = :installerId AND r.deletedAt IS NULL")
    Optional<Double> findAverageRatingByInstallerId(@Param("installerId") UUID installerId);
}