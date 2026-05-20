package com.solarride.solarride.controller;

import com.solarride.solarride.dto.request.CreateJobRequest;
import com.solarride.solarride.dto.request.RequestQuoteRequest;
import com.solarride.solarride.dto.request.SubmitQuoteRequest;
import com.solarride.solarride.dto.response.JobResponse;
import com.solarride.solarride.dto.response.QuoteResponse;
import com.solarride.solarride.security.SolarRideUserDetails;
import com.solarride.solarride.service.job.JobService;
import com.solarride.solarride.service.job.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final QuoteService quoteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse createJob(
            @Valid @RequestBody CreateJobRequest req,
            @AuthenticationPrincipal SolarRideUserDetails principal) {
        return jobService.createJob(principal.getUserId(), req);
    }

    @GetMapping("/{jobId}")
    public JobResponse getJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal SolarRideUserDetails principal) {
        return jobService.getJob(jobId, principal.getUserId());
    }

    @GetMapping
    public List<JobResponse> listMyJobs(
            @AuthenticationPrincipal SolarRideUserDetails principal) {
        return jobService.listCustomerJobs(principal.getUserId());
    }

    @DeleteMapping("/{jobId}")
    public JobResponse cancelJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal SolarRideUserDetails principal) {
        return jobService.cancelJob(jobId, principal.getUserId());
    }

    // --- Quote sub-resource ---

    @PostMapping("/{jobId}/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse requestQuote(
            @PathVariable UUID jobId,
            @Valid @RequestBody RequestQuoteRequest req,
            @AuthenticationPrincipal SolarRideUserDetails principal) {
        return quoteService.requestQuote(jobId, principal.getUserId(), req);
    }

    @GetMapping("/{jobId}/quotes")
    public List<QuoteResponse> listQuotes(@PathVariable UUID jobId) {
        return quoteService.listQuotes(jobId);
    }

    @PostMapping("/{jobId}/quotes/{quoteId}/accept")
    public QuoteResponse acceptQuote(
            @PathVariable UUID jobId,
            @PathVariable UUID quoteId,
            @AuthenticationPrincipal SolarRideUserDetails principal) {
        return quoteService.acceptQuote(jobId, quoteId, principal.getUserId());
    }
}