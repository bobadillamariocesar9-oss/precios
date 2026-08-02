package com.apiprecios.controller;

import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.entity.ScrapingJob.Status;
import com.apiprecios.service.ScrapingJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scraping-jobs")
@RequiredArgsConstructor
@Tag(name = "ScrapingJobs", description = "Gestión de jobs de scraping")
public class ScrapingJobController {

    private final ScrapingJobService scrapingJobService;

    @GetMapping
    @Operation(summary = "Listar todos los jobs de scraping")
    public List<ScrapingJob> findAll() {
        return scrapingJobService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener job por ID")
    public ScrapingJob findById(@PathVariable Integer id) {
        return scrapingJobService.findById(id);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Listar jobs por estado")
    public List<ScrapingJob> findByStatus(@PathVariable Status status) {
        return scrapingJobService.findByStatus(status);
    }

    @GetMapping("/store/{storeId}")
    @Operation(summary = "Listar jobs de una tienda")
    public List<ScrapingJob> findByStore(@PathVariable Integer storeId) {
        return scrapingJobService.findByStore(storeId);
    }

    @PostMapping
    @Operation(summary = "Crear nuevo job de scraping")
    public ResponseEntity<ScrapingJob> create(@Valid @RequestBody ScrapingJob job) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scrapingJobService.create(job));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Actualizar estado del job")
    public ScrapingJob updateStatus(@PathVariable Integer id, @RequestParam Status status) {
        return scrapingJobService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar job de scraping")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        scrapingJobService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
