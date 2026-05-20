package com.solarride.solarride.service.job;

import com.solarride.solarride.domain.job.Job;
import com.solarride.solarride.domain.job.JobStatus;
import com.solarride.solarride.exception.JobStateException;
import com.solarride.solarride.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Single authority for all Job status transitions.
 * No other service or controller may mutate job.status directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobLifecycleService {

    private final JobRepository jobRepository;

    @Transactional
    public Job transition(UUID jobId, JobStatus expectedCurrent, JobStatus newStatus) {
        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Job not found: " + jobId));
        if (job.getStatus() != expectedCurrent) {
            throw new JobStateException(
                    "Expected job status " + expectedCurrent + " but found " + job.getStatus());
        }
        log.info("Job {} transitioning {} -> {}", jobId, expectedCurrent, newStatus);
        job.setStatus(newStatus);
        if (newStatus == JobStatus.COMPLETED) {
            job.setCompletedAt(Instant.now());
        }
        return jobRepository.save(job);
    }

    @Transactional
    public Job markQuoted(UUID jobId) {
        return transition(jobId, JobStatus.DRAFT, JobStatus.QUOTED);
    }

    @Transactional
    public Job markConfirmed(UUID jobId) {
        return transition(jobId, JobStatus.QUOTED, JobStatus.CONFIRMED);
    }

    @Transactional
    public Job markInProgress(UUID jobId) {
        return transition(jobId, JobStatus.CONFIRMED, JobStatus.IN_PROGRESS);
    }

    @Transactional
    public Job markCompleted(UUID jobId) {
        return transition(jobId, JobStatus.IN_PROGRESS, JobStatus.COMPLETED);
    }

    @Transactional
    public Job markDisputed(UUID jobId) {
        return transition(jobId, JobStatus.COMPLETED, JobStatus.DISPUTED);
    }

    @Transactional
    public Job markPaid(UUID jobId) {
        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Job not found: " + jobId));
        if (job.getStatus() != JobStatus.COMPLETED && job.getStatus() != JobStatus.DISPUTED) {
            throw new JobStateException("Job must be COMPLETED or DISPUTED to mark PAID");
        }
        log.info("Job {} transitioning {} -> PAID", jobId, job.getStatus());
        job.setStatus(JobStatus.PAID);
        return jobRepository.save(job);
    }
}
