package com.apiprecios.controller;

import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.service.AgentPriceRefreshService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Narrow, token-protected tool endpoint for Botpress.
 * This does not expose arbitrary URLs, SQL, store edits or bulk scraping.
 */
@RestController
@RequestMapping("/api/agent/price-refresh")
@RequiredArgsConstructor
public class AgentPriceRefreshController {

    private final AgentPriceRefreshService refreshService;

    @Value("${agent.api-token:}")
    private String apiToken;

    public record RefreshRequest(
            @NotNull @Positive Integer productId,
            String source,
            Boolean dryRun
    ) {}

    public record RefreshAccepted(Integer jobId, ScrapingJob.Status status, boolean dryRun) {}

    @PostMapping
    public ResponseEntity<RefreshAccepted> requestRefresh(
            @RequestHeader(value = "X-Agent-Token", required = false) String presentedToken,
            @Valid @RequestBody RefreshRequest request) {
        requireToken(presentedToken);
        return ResponseEntity.accepted().body(refreshService.start(
                request.productId(), request.source(), request.dryRun()));
    }

    @GetMapping("/{id}")
    public ScrapingJob getStatus(
            @RequestHeader(value = "X-Agent-Token", required = false) String presentedToken,
            @PathVariable Integer id) {
        requireToken(presentedToken);
        return refreshService.findById(id);
    }

    private void requireToken(String presentedToken) {
        if (apiToken == null || apiToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Botpress integration has not been configured");
        }
        byte[] configured = apiToken.getBytes(StandardCharsets.UTF_8);
        byte[] presented = presentedToken == null
                ? new byte[0] : presentedToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(configured, presented)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid agent token");
        }
    }
}
