package com.apiprecios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class SepaAmbaPriceServiceTest {

    private final ObjectMapper json = new ObjectMapper();

    private String payload(String ean, String region, String date) {
        return "{\"ean\":\"" + ean + "\",\"region\":\"" + region
                + "\",\"fecha\":\"" + date
                + "\",\"precios\":[{\"cadena\":{\"nombre\":\"Coto\"},\"precio_lista\":2500}]}";
    }

    @Test
    void validatesEan13CheckDigit() {
        assertTrue(SepaAmbaPriceService.isValidEan13("7790139101169"));
        assertFalse(SepaAmbaPriceService.isValidEan13("7790139101168"));
        assertFalse(SepaAmbaPriceService.isValidEan13("leche"));
        assertFalse(SepaAmbaPriceService.isValidEan13(null));
    }

    @Test
    void acceptsRecentAmbaDataMatchingEan() throws Exception {
        String today = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString();
        var parsed = json.readTree(payload("7790139101169", "AMBA", today));
        assertEquals(LocalDate.parse(today),
                SepaAmbaPriceService.validateHeader(parsed, "7790139101169"));
    }

    @Test
    void rejectsWrongEanOrRegion() throws Exception {
        String today = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString();
        assertThrows(IllegalArgumentException.class,
                () -> SepaAmbaPriceService.validateHeader(
                        json.readTree(payload("7790139101169", "AMBA", today)),
                        "7790139101168"));
        assertThrows(IllegalArgumentException.class,
                () -> SepaAmbaPriceService.validateHeader(
                        json.readTree(payload("7790139101169", "NACIONAL", today)),
                        "7790139101169"));
    }

    @Test
    void rejectsStaleData() throws Exception {
        String old = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
                .minusDays(10).toString();
        assertThrows(IllegalArgumentException.class,
                () -> SepaAmbaPriceService.validateHeader(
                        json.readTree(payload("7790139101169", "AMBA", old)),
                        "7790139101169"));
    }

    @Test
    void ignoresUnrealisticAndNonNumericPrices() throws Exception {
        assertNull(SepaAmbaPriceService.parsePrice(json.readTree("-1")));
        assertNull(SepaAmbaPriceService.parsePrice(json.readTree("0")));
        assertNull(SepaAmbaPriceService.parsePrice(json.readTree("100000001")));
        assertNull(SepaAmbaPriceService.parsePrice(json.readTree("\"unavailable\"")));
        assertEquals(0,
                SepaAmbaPriceService.parsePrice(json.readTree("2500")).compareTo(
                        new java.math.BigDecimal("2500")));
    }
}
