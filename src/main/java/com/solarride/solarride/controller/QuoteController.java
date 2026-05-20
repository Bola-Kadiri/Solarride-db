package com.solarride.solarride.controller;

import com.solarride.solarride.dto.request.SubmitQuoteRequest;
import com.solarride.solarride.dto.response.QuoteResponse;
import com.solarride.solarride.security.SolarRideUserDetails;
import com.solarride.solarride.service.job.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/installer/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/{quoteId}/jobs/{jobId}/submit")
    public QuoteResponse submitQuote(
            @PathVariable UUID quoteId,
            @PathVariable UUID jobId,
            @Valid @RequestBody SubmitQuoteRequest req,
            @AuthenticationPrincipal SolarRideUserDetails principal) {
        return quoteService.submitQuote(jobId, quoteId, principal.getUserId(), req);
    }
}
