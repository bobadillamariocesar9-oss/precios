package com.apiprecios.controller;

import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.service.CotoScraperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scraper")
@RequiredArgsConstructor
@Tag(name = "Scraper", description = "Disparo manual de scraping real de precios")
public class ScraperController {

    private final CotoScraperService cotoScraperService;

    @PostMapping("/coto/run")
    @Operation(summary = "Ejecuta el scraping real de Coto Digital para todos los productos. " +
            "Corre un Chrome headless en el servidor — puede tardar varios minutos según la cantidad de productos.")
    public ScrapingJob runCotoScraper() {
        return cotoScraperService.scrapeAllProducts();
    }
}
