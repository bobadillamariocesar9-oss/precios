package com.apiprecios.service;

import com.apiprecios.entity.Product;
import com.apiprecios.entity.ScrapingJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentPriceRefreshServiceTest {

    private ProductService products;
    private ScrapingJobService jobs;
    private AgentPriceRefreshWorker worker;
    private AgentPriceRefreshService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductService.class);
        jobs = mock(ScrapingJobService.class);
        worker = mock(AgentPriceRefreshWorker.class);
        service = new AgentPriceRefreshService(products, jobs, worker);
        ReflectionTestUtils.setField(service, "liveScrapingEnabled", false);
    }

    @Test
    void simulationIsDefaultAndNeverRequestsLiveScraping() {
        when(products.findById(7)).thenReturn(Product.builder().id(7).build());
        when(jobs.create(any(ScrapingJob.class)))
                .thenReturn(ScrapingJob.builder().id(42).status(ScrapingJob.Status.PENDING).build());

        var response = service.start(7, "COTO_DIGITAL", null);

        assertEquals(42, response.jobId());
        assertTrue(response.dryRun());
        verify(worker).execute(42, 7, true);
    }

    @Test
    void realScrapingRequiresFeatureFlag() {
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.start(7, "COTO_DIGITAL", false));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(products, jobs, worker);
    }

    @Test
    void rejectsUnknownSource() {
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.start(7, "UNTRUSTED_URL", true));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(products, jobs, worker);
    }
}
