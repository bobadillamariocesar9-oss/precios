package com.apiprecios.repository;

import com.apiprecios.entity.ScrapingLog;
import com.apiprecios.entity.ScrapingLog.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScrapingLogRepository extends JpaRepository<ScrapingLog, Integer> {

    List<ScrapingLog> findByScrapingJobId(Integer jobId);

    List<ScrapingLog> findByScrapingJobIdAndLevel(Integer jobId, Level level);

    List<ScrapingLog> findByScrapingJobIdOrderByCreatedAtDesc(Integer jobId);

    List<ScrapingLog> findAllByOrderByCreatedAtDesc();
}
