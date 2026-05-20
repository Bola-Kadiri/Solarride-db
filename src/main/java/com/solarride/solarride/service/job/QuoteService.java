package com.solarride.solarride.service.job;

import com.solarride.solarride.domain.installer.Installer;
import com.solarride.solarride.domain.job.Job;
import com.solarride.solarride.domain.job.JobStatus;
import com.solarride.solarride.domain.job.Quote;
import com.solarride.solarride.domain.job.QuoteStatus;
import com.solarride.solarride.dto.request.RequestQuoteRequest;
import com.solarride.solarride.dto.request.SubmitQuoteRequest;
import com.solarride.solarride.dto.response.QuoteResponse;
import com.solarride.solarride.exception.JobStateException;
import com.solarride.solarride.repository.InstallerRepository;
import com.solarride.solarride.repository.JobRepository;
import com.solarride.solarride.repository.QuoteRepository;
import com.solarride.solarride.service.notification.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteService {

    private static final int MAX_QUOTES_PER_JOB = 3;
    private static final int QUOTE_SLA_HOURS = 24;

    private final QuoteRepository quoteRepository;
    private final JobRepository jobRepository;
    private final InstallerRepository installerRepository;
    private final JobLifecycleService jobLifecycleService;
    private final NotificationOrchestrator notificationOrchestrator;

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public QuoteResponse requestQuote(UUID jobId, UUID customerId, RequestQuoteRequest req) {
        Job job = findActiveJob(jobId);

        if (!job.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("You do not own this job");
        }
        if (job.getStatus() != JobStatus.DRAFT && job.getStatus() != JobStatus.QUOTED) {
            throw new JobStateException("Quotes can only be requested on DRAFT or QUOTED jobs");
        }
        long existingQuotes = quoteRepository.countByJobIdAndDeletedAtIsNull(jobId);
        if (existingQuotes >= MAX_QUOTES_PER_JOB) {
            throw new JobStateException("Maximum of " + MAX_QUOTES_PER_JOB + " quote requests per job");
        }
        if (quoteRepository.existsByJobIdAndInstallerIdAndDeletedAtIsNull(jobId, req.installerId())) {
            throw new JobStateException("Quote already requested from this installer");
        }

        Installer installer = installerRepository.findById(req.installerId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Installer not found: " + req.installerId()));

        Quote quote = new Quote();
        quote.setJob(job);
        quote.setInstaller(installer);
        quote.setStatus(QuoteStatus.REQUESTED);
        quote.setSlaDeadlineAt(Instant.now().plus(QUOTE_SLA_HOURS, ChronoUnit.HOURS));
        quote = quoteRepository.save(quote);

        log.info("Quote {} requested for job {} from installer {}", quote.getId(), jobId, req.installerId());
        notificationOrchestrator.notify(
                installer.getUser().getId(), "QUOTE_REQUESTED",
                "You have a new quote request for job " + jobId);

        return toResponse(quote);
    }

    @Transactional
    @PreAuthorize("hasRole('INSTALLER')")
    public QuoteResponse submitQuote(UUID jobId, UUID quoteId, UUID installerId, SubmitQuoteRequest req) {
        Quote quote = findActiveQuote(quoteId);

        if (!quote.getJob().getId().equals(jobId)) {
            throw new AccessDeniedException("Quote does not belong to this job");
        }
        if (!quote.getInstaller().getUser().getId().equals(installerId)) {
            throw new AccessDeniedException("You are not the installer for this quote");
        }
        if (quote.getStatus() != QuoteStatus.REQUESTED) {
            throw new JobStateException("Quote is not in REQUESTED status");
        }
        if (Instant.now().isAfter(quote.getSlaDeadlineAt())) {
            log.warn("Quote {} submitted after SLA deadline", quoteId);
        }

        quote.setLabourCost(req.labourCost());
        quote.setEstimatedPartsCost(req.estimatedPartsCost());
        quote.setTotalCost(req.labourCost().add(req.estimatedPartsCost()));
        quote.setProposedStartDate(req.proposedStartDate());
        quote.setProposedEndDate(req.proposedEndDate());
        quote.setNotes(req.notes());
        quote.setStatus(QuoteStatus.SUBMITTED);
        quote = quoteRepository.save(quote);

        // Advance job to QUOTED if still in DRAFT
        Job job = quote.getJob();
        if (job.getStatus() == JobStatus.DRAFT) {
            jobLifecycleService.markQuoted(job.getId());
        }

        log.info("Quote {} submitted for job {} total={}", quoteId, jobId, quote.getTotalCost());
        notificationOrchestrator.notify(
                job.getCustomer().getId(), "QUOTE_SUBMITTED",
                "An installer has submitted a quote for your job");

        return toResponse(quote);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('INSTALLER') or hasRole('ADMIN')")
    public List<QuoteResponse> listQuotes(UUID jobId) {
        return quoteRepository.findByJobIdAndDeletedAtIsNull(jobId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public QuoteResponse acceptQuote(UUID jobId, UUID quoteId, UUID customerId) {
        Job job = findActiveJob(jobId);

        if (!job.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("You do not own this job");
        }
        if (job.getStatus() != JobStatus.QUOTED) {
            throw new JobStateException("Job must be in QUOTED status to accept a quote");
        }

        Quote accepted = findActiveQuote(quoteId);
        if (!accepted.getJob().getId().equals(jobId)) {
            throw new AccessDeniedException("Quote does not belong to this job");
        }
        if (accepted.getStatus() != QuoteStatus.SUBMITTED) {
            throw new JobStateException("Only SUBMITTED quotes can be accepted");
        }

        // Decline all other submitted quotes for this job
        quoteRepository.findByJobIdAndStatus(jobId, QuoteStatus.SUBMITTED).forEach(q -> {
            if (!q.getId().equals(quoteId)) {
                q.setStatus(QuoteStatus.DECLINED);
                quoteRepository.save(q);
                log.info("Quote {} declined (another accepted)", q.getId());
            }
        });

        accepted.setStatus(QuoteStatus.ACCEPTED);
        accepted = quoteRepository.save(accepted);

        // Assign installer to job
        job.setInstaller(accepted.getInstaller());
        job.setJobValue(accepted.getTotalCost());
        jobRepository.save(job);

        log.info("Quote {} accepted for job {} — installer {}", quoteId, jobId,
                accepted.getInstaller().getId());
        notificationOrchestrator.notify(
                accepted.getInstaller().getUser().getId(), "QUOTE_ACCEPTED",
                "Your quote for job " + jobId + " has been accepted! Awaiting payment.");

        return toResponse(accepted);
    }

    private Job findActiveJob(UUID jobId) {
        return jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Job not found: " + jobId));
    }

    private Quote findActiveQuote(UUID quoteId) {
        return quoteRepository.findByIdAndDeletedAtIsNull(quoteId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Quote not found: " + quoteId));
    }

    QuoteResponse toResponse(Quote q) {
        var installer = q.getInstaller();
        var user = installer.getUser();
        return new QuoteResponse(
                q.getId(),
                q.getJob().getId(),
                installer.getId(),
                user.getFirstName() + " " + user.getLastName(),
                installer.getCompanyName(),
                q.getStatus().name(),
                q.getLabourCost(),
                q.getEstimatedPartsCost(),
                q.getTotalCost(),
                q.getProposedStartDate(),
                q.getProposedEndDate(),
                q.getNotes(),
                q.getSlaDeadlineAt(),
                q.getCreatedAt());
    }
}
