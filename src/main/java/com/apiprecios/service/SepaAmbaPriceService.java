package com.apiprecios.service;

import com.apiprecios.entity.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Prototype connector: reads small per-EAN regional summaries derived from the
 * official SEPA open dataset. This is NOT the official SEPA API and it is NOT
 * a price for any individual branch. Requires explicit operator opt-in.
 */
@Service
@RequiredArgsConstructor
public class SepaAmbaPriceService {

    private static final String PROVIDER_BASE =
            "https://hysmarcos.github.io/superprecios-claros/v1/sku/";
    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final int MAX_DATA_AGE_DAYS = 2;
    private static final BigDecimal MAX_REASONABLE_PRICE = new BigDecimal("100000000");

    private final ProductService products;
    private final SepaPersistenceService persistence;
    private final ScrapingLogService logs;
    private final ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public void refreshOneProduct(Integer productId, Integer jobId) throws Exception {
        Product product = products.findById(productId);
        String ean = product.getEan();
        if (!isValidEan13(ean)) {
            throw new IllegalArgumentException("Product " + productId
                    + " needs a valid EAN-13 before real SEPA import");
        }

        // Only a trusted fixed host and a checked numeric EAN enter the URL.
        String sourceUrl = PROVIDER_BASE + ean + "/AMBA.json";
        HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = http.send(
                request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("SEPA regional provider HTTP " + response.statusCode());
        }
        if (response.body().length() > 2_000_000) {
            throw new IllegalStateException("SEPA response exceeds size limit");
        }

        JsonNode payload = objectMapper.readTree(response.body());
        LocalDate date = validateHeader(payload, ean);
        JsonNode entries = payload.path("precios");
        var observations = new java.util.ArrayList<SepaPersistenceService.Observation>();
        int rejected = 0;
        for (JsonNode entry : entries) {
            String chain = entry.path("cadena").path("nombre").asText("").trim();
            BigDecimal listPrice = parsePrice(entry.path("precio_lista"));
            if (chain.isEmpty() || chain.length() > 100 || listPrice == null) {
                rejected++;
                continue;
            }
            observations.add(new SepaPersistenceService.Observation(chain, listPrice));
        }
        var result = persistence.persistAmba(productId, ean, date, sourceUrl, observations);
        logs.info(jobId, "SEPA AMBA " + date + ": new observations=" + result.saved()
                + ", unchanged=" + result.unchanged() + ", corrected=" + result.corrected()
                + ", rejected=" + rejected);
    }

    static LocalDate validateHeader(JsonNode payload, String ean) {
        if (!ean.equals(payload.path("ean").asText())) {
            throw new IllegalArgumentException("Provider EAN differs from requested EAN");
        }
        if (!"AMBA".equals(payload.path("region").asText())) {
            throw new IllegalArgumentException("Provider did not return AMBA region");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(payload.path("fecha").asText());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Missing or invalid source date", ex);
        }
        LocalDate today = LocalDate.now(ARGENTINA);
        if (date.isAfter(today) || date.isBefore(today.minusDays(MAX_DATA_AGE_DAYS))) {
            throw new IllegalArgumentException("SEPA data is stale or future-dated: " + date);
        }
        if (!payload.path("precios").isArray()) {
            throw new IllegalArgumentException("Missing regional prices array");
        }
        return date;
    }

    static BigDecimal parsePrice(JsonNode value) {
        if (!value.isNumber()) return null;
        BigDecimal result = value.decimalValue();
        if (result.compareTo(BigDecimal.ONE) < 0
                || result.compareTo(MAX_REASONABLE_PRICE) > 0
                || result.scale() > 2) return null;
        return result;
    }

    static boolean isValidEan13(String ean) {
        if (ean == null || !ean.matches("[0-9]{13}")) return false;
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = ean.charAt(i) - '0';
            sum += digit * (i % 2 == 0 ? 1 : 3);
        }
        int check = (10 - (sum % 10)) % 10;
        return check == ean.charAt(12) - '0';
    }
}
