package com.apiprecios.service;

import com.apiprecios.entity.*;
import com.apiprecios.repository.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SepaPersistenceServiceTest {
    EntityManager em = mock(EntityManager.class);
    StoreRepository stores = mock(StoreRepository.class);
    PriceRepository prices = mock(PriceRepository.class);
    SepaPersistenceService service = new SepaPersistenceService(em, stores, prices);
    TypedQuery<Price> query;
    LocalDate date = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
    String ean = "7790139101169";
    String url = "https://hysmarcos.github.io/superprecios-claros/v1/sku/" + ean + "/AMBA.json";
    List<SepaPersistenceService.Observation> observations =
            List.of(new SepaPersistenceService.Observation("Coto", new BigDecimal("2500.00")));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        when(em.find(Product.class, 1, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(Product.builder().id(1).ean(ean).build());
        Query lock = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(lock);
        when(lock.setParameter(anyString(), any())).thenReturn(lock);
        when(stores.findByName("SEPA AMBA | Coto"))
                .thenReturn(Optional.of(Store.builder().id(2).build()));
        query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Price.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
    }

    @Test
    void savesAmountAndProvenanceWithSourceDayOrdering() {
        assertEquals(new SepaPersistenceService.Result(1, 0, 0),
                service.persistAmba(1, ean, date, url, observations));
        var captor = org.mockito.ArgumentCaptor.forClass(Price.class);
        verify(prices).saveAndFlush(captor.capture());
        Price saved = captor.getValue();
        assertEquals("ARS", saved.getCurrency());
        assertEquals("AMBA_CHAIN_MEDIAN", saved.getPriceScope());
        assertEquals(url, saved.getSourceUrl());
        assertEquals(date, saved.getSourceDate());
        assertEquals(date.atStartOfDay(), saved.getRecordedAt());
    }

    @Test
    void repeatedFileDoesNotAppendAnObservation() {
        Price old = Price.builder().price(new BigDecimal("2500")).build();
        when(query.getResultList()).thenReturn(List.of(old));
        assertEquals(new SepaPersistenceService.Result(0, 1, 0),
                service.persistAmba(1, ean, date, url, observations));
        verifyNoInteractions(prices);
    }

    @Test
    void correctionUpdatesSameObservation() {
        Price old = Price.builder().price(new BigDecimal("2400"))
                .recordedAt(date.atStartOfDay()).build();
        when(query.getResultList()).thenReturn(List.of(old));
        assertEquals(new SepaPersistenceService.Result(0, 0, 1),
                service.persistAmba(1, ean, date, url, observations));
        assertEquals(new BigDecimal("2500.00"), old.getPrice());
        assertEquals(date.atStartOfDay(), old.getRecordedAt());
        verifyNoInteractions(prices);
    }

    @Test
    void rejectsMismatchedProductBeforeWriting() {
        when(em.find(Product.class, 1, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(Product.builder().id(1).ean("other").build());
        assertThrows(IllegalArgumentException.class,
                () -> service.persistAmba(1, ean, date, url, observations));
        verifyNoInteractions(stores, prices);
    }

    @Test
    void rejectsInvalidBatchBeforeDatabaseAccess() {
        var invalid = List.of(new SepaPersistenceService.Observation("Coto", BigDecimal.ZERO));
        clearInvocations(em, stores, prices);
        assertThrows(IllegalArgumentException.class,
                () -> service.persistAmba(1, ean, date, url, invalid));
        assertThrows(IllegalArgumentException.class,
                () -> service.persistAmba(1, ean, date.minusDays(3), url, observations));
        assertThrows(IllegalArgumentException.class,
                () -> service.persistAmba(1, ean, date, "https://example.com", observations));
        verifyNoInteractions(em, stores, prices);
    }
}
