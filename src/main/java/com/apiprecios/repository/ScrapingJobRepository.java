package com.apiprecios.repository;

import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.entity.ScrapingJob.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScrapingJobRepository extends JpaRepository<ScrapingJob, Integer> {

    List<ScrapingJob> findByStatus(Status status);

    List<ScrapingJob> findByStoreId(Integer storeId);

    List<ScrapingJob> findByStoreIdAndStatus(Integer storeId, Status status);
}
