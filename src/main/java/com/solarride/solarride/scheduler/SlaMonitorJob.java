package com.solarride.solarride.scheduler;

import com.solarride.solarride.domain.job.QuoteStatus;
import com.solarride.solarride.repository.QuoteRepository;
import com.solarride.solarride.service.notification.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Fires every 10 minutes. Detects quotes that breached their 24-hour SLA
 * and marks them EXPIRED so the job can be redistributed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaMonitorJob implements Job {

    private final QuoteRepository quoteRepository;
    private final NotificationOrchestrator notificationOrchestrator;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) {
        Instant now = Instant.now();
        var expired = quoteRepository.findOverdueRequestedQuotes(now);
        if (expired.isEmpty()) return;

        log.warn("SLA breach: {} quote(s) overdue", expired.size());
        for (var quote : expired) {
            quote.setStatus(QuoteStatus.EXPIRED);
            quoteRepository.save(quote);
            notificationOrchestrator.notify(
                    quote.getInstaller().getUser().getId(),
                    "SLA_BREACH_QUOTE",
                    "Your quote response window for job " + quote.getJob().getId() + " has expired.");
            log.warn("Quote {} expired (SLA breach) for installer {}",
                    quote.getId(), quote.getInstaller().getId());
        }
    }
}
