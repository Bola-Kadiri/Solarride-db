package com.solarride.solarride.service;

import com.solarride.solarride.domain.job.Job;
import com.solarride.solarride.domain.job.JobStatus;
import com.solarride.solarride.exception.JobStateException;
import com.solarride.solarride.repository.JobRepository;
import com.solarride.solarride.service.job.JobLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobLifecycleServiceTest {

    @Mock
    JobRepository jobRepository;

    @InjectMocks
    JobLifecycleService lifecycleService;

    private Job job;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        job = new Job();
        job.setStatus(JobStatus.DRAFT);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
    }

    @Test
    void markQuoted_fromDraft_succeeds() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        Job result = lifecycleService.markQuoted(jobId);
        assertThat(result.getStatus()).isEqualTo(JobStatus.QUOTED);
    }

    @Test
    void markQuoted_fromWrongStatus_throws() {
        job.setStatus(JobStatus.CONFIRMED);
        assertThatThrownBy(() -> lifecycleService.markQuoted(jobId))
                .isInstanceOf(JobStateException.class)
                .hasMessageContaining("Expected job status DRAFT");
    }

    @Test
    void markConfirmed_fromQuoted_succeeds() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        job.setStatus(JobStatus.QUOTED);
        Job result = lifecycleService.markConfirmed(jobId);
        assertThat(result.getStatus()).isEqualTo(JobStatus.CONFIRMED);
    }

    @Test
    void markInProgress_fromConfirmed_succeeds() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        job.setStatus(JobStatus.CONFIRMED);
        Job result = lifecycleService.markInProgress(jobId);
        assertThat(result.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
    }

    @Test
    void markCompleted_fromInProgress_setsCompletedAt() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        job.setStatus(JobStatus.IN_PROGRESS);
        Job result = lifecycleService.markCompleted(jobId);
        assertThat(result.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
    }
}
