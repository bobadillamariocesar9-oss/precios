package com.apiprecios.service;

import com.apiprecios.entity.ScrapingJob.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Path;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Executes the slow browser work off the HTTP request thread. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPriceRefreshWorker {

    private final ScrapingJobService jobs;
    private final ScrapingLogService logs;
    private final SepaAmbaPriceService sepaAmbaPriceService;

    private final ProductService products;
    private final SepaPersistenceService persistence;

    @Value("${agent.sepa-official-zip:}")
    private String officialZip;

    @Value("${agent.sepa-official-province:AR-C}")
    private String officialProvince;

    @Async("priceRefreshExecutor")
    public void execute(Integer jobId, Integer productId, boolean dryRun) {
        try {
            jobs.updateStatus(jobId, Status.RUNNING);
            if (dryRun) {
                logs.info(jobId, "Botpress development simulation for product " + productId
                        + ": no network scraping and no price changes");
            } else if (!officialZip.isBlank()) {
                String ean = products.findById(productId).getEan();
                if (!SepaAmbaPriceService.isValidEan13(ean)) {
                    throw new IllegalArgumentException("Official SEPA import requires a valid product EAN-13");
                }
                var observations = SepaOfficialArchive.read(Path.of(officialZip), ean, officialProvince);
                var result = persistence.persistOfficial(productId, ean, observations);
                logs.info(jobId, "Official SEPA " + officialProvince + ": new=" + result.saved()
                        + ", unchanged=" + result.unchanged() + ", corrected=" + result.corrected());
            } else {
                sepaAmbaPriceService.refreshOneProduct(productId, jobId);
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
