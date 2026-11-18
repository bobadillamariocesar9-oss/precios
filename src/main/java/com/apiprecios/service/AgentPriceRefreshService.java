package com.apiprecios.service;

import com.apiprecios.controller.AgentPriceRefreshController.RefreshAccepted;
import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.entity.ScrapingJob.Status;
import com.apiprecios.repository.ScrapingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Creates one small, observable job at a time; real writes are opt-in. */
@Service
@RequiredArgsConstructor
public class AgentPriceRefreshService {

    private final ProductService productService;
    private final ScrapingJobService jobService;
    private final AgentPriceRefreshWorker worker;

    @Value("${agent.live-scraping-enabled:false}")
    private boolean liveScrapingEnabled;

    public RefreshAccepted start(Integer productId, String source, Boolean dryRunFlag) {
        if (source != null && !source.equals("COTO_DIGITAL")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported price source");
        }

        boolean dryRun = !Boolean.FALSE.equals(dryRunFlag);
        if (!dryRun && !liveScrapingEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Live scraping is disabled in this development environment");
        }

        productService.findById(productId); // reject unknown products before scheduling
        ScrapingJob job = jobService.create(ScrapingJob.builder()
                .url("https://www.cotodigital.com.ar/sitios/cdigi/browse")
                .build());

        try {
            worker.execute(job.getId(), productId, dryRun);
        } catch (RuntimeException schedulingError) {
            jobService.updateStatus(job.getId(), Status.FAILED);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Price-refresh queue is unavailable", schedulingError);
        }
        return new RefreshAccepted(job.getId(), Status.PENDING, dryRun);
    }

    public ScrapingJob findById(Integer id) {
        return jobService.findById(id);
    }
}
