package com.apiprecios.service;

import com.apiprecios.entity.ScrapingJob.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Executes the slow browser work off the HTTP request thread. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPriceRefreshWorker {

    private final ScrapingJobService jobs;
    private final ScrapingLogService logs;
    private final CotoScraperService cotoScraper;

    @Async("priceRefreshExecutor")
    public void execute(Integer jobId, Integer productId, boolean dryRun) {
        try {
            jobs.updateStatus(jobId, Status.RUNNING);
            if (dryRun) {
                logs.info(jobId, "Botpress development simulation for product " + productId
                        + ": no network scraping and no price changes");
            } else {
                cotoScraper.scrapeProductForJob(productId, jobId);
            }
            jobs.updateStatus(jobId, Status.COMPLETED);
        } catch (Exception ex) {
            log.error("Botpress price refresh failed for job {}", jobId, ex);
            try {
                logs.error(jobId, "Price refresh failed: " + ex.getClass().getSimpleName());
                jobs.updateStatus(jobId, Status.FAILED);
            } catch (Exception statusEx) {
                log.error("Could not mark job {} as FAILED", jobId, statusEx);
            }
        }
    }
}
