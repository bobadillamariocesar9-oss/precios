package com.apiprecios.controller;

import com.apiprecios.entity.ScrapingLog;
import com.apiprecios.entity.ScrapingLog.Level;
import com.apiprecios.service.ScrapingLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scraping-logs")
@RequiredArgsConstructor
@Tag(name = "ScrapingLogs", description = "Gestión de logs de scraping")
public class ScrapingLogController {

    private final ScrapingLogService scrapingLogService;

    @GetMapping
    @Operation(summary = "Listar todos los logs")
    public List<ScrapingLog> findAll() {
        return scrapingLogService.findAll();
    }

    @GetMapping("/job/{jobId}")
    @Operation(summary = "Listar logs de un job")
    public List<ScrapingLog> findByJob(@PathVariable Integer jobId) {
        return scrapingLogService.findByJob(jobId);
    }

    @GetMapping("/job/{jobId}/level/{level}")
    @Operation(summary = "Listar logs de un job filtrados por nivel")
    public List<ScrapingLog> findByJobAndLevel(@PathVariable Integer jobId, @PathVariable Level level) {
        return scrapingLogService.findByJobAndLevel(jobId, level);
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo log")
    public ResponseEntity<ScrapingLog> log(
            @RequestParam Integer jobId,
            @RequestParam String message,
            @RequestParam(required = false) Level level) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scrapingLogService.log(jobId, message, level));
    }
}
