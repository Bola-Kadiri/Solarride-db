package com.solarride.solarride.service.job;

import com.solarride.solarride.domain.job.Job;
import com.solarride.solarride.domain.job.JobStatus;
import com.solarride.solarride.domain.user.User;
import com.solarride.solarride.dto.request.CreateJobRequest;
import com.solarride.solarride.dto.response.JobResponse;
import com.solarride.solarride.exception.JobStateException;
import com.solarride.solarride.repository.JobRepository;
import com.solarride.solarride.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CostEstimationService costEstimationService;

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public JobResponse createJob(UUID customerId, CreateJobRequest req) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + customerId));

        Point location = GF.createPoint(new Coordinate(req.longitude(), req.latitude()));
        location.setSRID(4326);

        Job job = new Job();
        job.setCustomer(customer);
        job.setSolarSize(req.solarSize());
        job.setStatus(JobStatus.DRAFT);
        job.setPropertyType(req.propertyType());
        job.setPropertyAddress(req.propertyAddress());
        job.setPropertyLocation(location);
        job.setPreferredStartDate(req.preferredStartDate());

        var estimate = costEstimationService.estimate(req.solarSize(), req.latitude(), req.longitude());
        job.setEstimatedCostMin(estimate.estimatedCostMin());
        job.setEstimatedCostMax(estimate.estimatedCostMax());

        job = jobRepository.save(job);
        log.info("Job {} created by customer {} with solarSize={}", job.getId(), customerId, req.solarSize());
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public JobResponse getJob(UUID jobId, UUID requesterId) {
        Job job = findActiveJob(jobId);
        if (!job.getCustomer().getId().equals(requesterId) &&
                !userRepository.findById(requesterId)
                        .map(u -> u.getRole().name().equals("ADMIN")).orElse(false)) {
            throw new AccessDeniedException("You do not have access to this job");
        }
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<JobResponse> listCustomerJobs(UUID customerId) {
        return jobRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public JobResponse cancelJob(UUID jobId, UUID customerId) {
        Job job = findActiveJob(jobId);
        if (!job.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("You do not own this job");
        }
        if (job.getStatus() != JobStatus.DRAFT && job.getStatus() != JobStatus.QUOTED) {
            throw new JobStateException("Job can only be cancelled from DRAFT or QUOTED status");
        }
        job.setStatus(JobStatus.CANCELLED);
        log.info("Job {} cancelled by customer {}", jobId, customerId);
        return toResponse(jobRepository.save(job));
    }

    public Job findActiveJob(UUID jobId) {
        return jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Job not found: " + jobId));
    }

    JobResponse toResponse(Job job) {
        Double lat = null;
        Double lng = null;
        if (job.getPropertyLocation() != null) {
            lat = job.getPropertyLocation().getY();
            lng = job.getPropertyLocation().getX();
        }
        return new JobResponse(
                job.getId(),
                job.getCustomer().getId(),
                job.getInstaller() != null ? job.getInstaller().getId() : null,
                job.getSolarSize().name(),
                job.getStatus().name(),
                job.getPaymentPlan() != null ? job.getPaymentPlan().name() : null,
                job.getPropertyType(),
                job.getPropertyAddress(),
                lat,
                lng,
                job.getPreferredStartDate(),
                job.getEstimatedCostMin(),
                job.getEstimatedCostMax(),
                job.getJobValue(),
                job.getCreatedAt());
    }
}