package com.apiprecios.service;

import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.entity.ScrapingLog;
import com.apiprecios.entity.ScrapingLog.Level;
import com.apiprecios.repository.ScrapingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapingLogService {

    private final ScrapingLogRepository scrapingLogRepository;
    private final ScrapingJobService scrapingJobService;

    public List<ScrapingLog> findAll() {
        return scrapingLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ScrapingLog> findByJob(Integer jobId) {
        scrapingJobService.findById(jobId);
        return scrapingLogRepository.findByScrapingJobIdOrderByCreatedAtDesc(jobId);
    }

    public List<ScrapingLog> findByJobAndLevel(Integer jobId, Level level) {
        scrapingJobService.findById(jobId);
        return scrapingLogRepository.findByScrapingJobIdAndLevel(jobId, level);
    }

    @Transactional
    public ScrapingLog log(Integer jobId, String message, Level level) {
        ScrapingJob job = scrapingJobService.findById(jobId);
        return scrapingLogRepository.save(ScrapingLog.builder()
                .scrapingJob(job)
                .message(message)
                .level(level != null ? level : Level.INFO)
                .build());
    }

    @Transactional
    public ScrapingLog info(Integer jobId, String message) {
        return log(jobId, message, Level.INFO);
    }

    @Transactional
    public ScrapingLog warn(Integer jobId, String message) {
        return log(jobId, message, Level.WARN);
    }

    @Transactional
    public ScrapingLog error(Integer jobId, String message) {
        return log(jobId, message, Level.ERROR);
    }
}
