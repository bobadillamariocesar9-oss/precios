package com.apiprecios.service;

import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.entity.ScrapingJob.Status;
import com.apiprecios.exception.BadRequestException;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.ScrapingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapingJobService {

    private final ScrapingJobRepository scrapingJobRepository;
    private final StoreService storeService;

    public List<ScrapingJob> findAll() {
        return scrapingJobRepository.findAll();
    }

    public ScrapingJob findById(Integer id) {
        return scrapingJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScrapingJob", id));
    }

    public List<ScrapingJob> findByStatus(Status status) {
        return scrapingJobRepository.findByStatus(status);
    }

    public List<ScrapingJob> findByStore(Integer storeId) {
        storeService.findById(storeId);
        return scrapingJobRepository.findByStoreId(storeId);
    }

    @Transactional
    public ScrapingJob create(ScrapingJob job) {
        if (job.getStore() != null && job.getStore().getId() != null) {
            storeService.findById(job.getStore().getId());
        }
        job.setStatus(Status.PENDING);
        return scrapingJobRepository.save(job);
    }

    @Transactional
    public ScrapingJob updateStatus(Integer id, Status newStatus) {
        ScrapingJob job = findById(id);

        if (job.getStatus() == Status.COMPLETED || job.getStatus() == Status.FAILED) {
            throw new BadRequestException(
                    "No se puede cambiar el estado de un job en estado " + job.getStatus());
        }

        job.setStatus(newStatus);
        if (newStatus == Status.RUNNING) {
            job.setStartedAt(LocalDateTime.now());
        } else if (newStatus == Status.COMPLETED || newStatus == Status.FAILED) {
            job.setFinishedAt(LocalDateTime.now());
        }
        return scrapingJobRepository.save(job);
    }

    @Transactional
    public void delete(Integer id) {
        ScrapingJob job = findById(id);
        scrapingJobRepository.delete(job);
    }
}
