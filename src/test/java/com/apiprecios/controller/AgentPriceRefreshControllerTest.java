package com.apiprecios.controller;

import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.service.AgentPriceRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentPriceRefreshControllerTest {

    private AgentPriceRefreshService service;
    private AgentPriceRefreshController controller;

    @BeforeEach
    void setUp() {
        service = mock(AgentPriceRefreshService.class);
        controller = new AgentPriceRefreshController(service);
        ReflectionTestUtils.setField(controller, "apiToken", "test-secret-not-for-production");
    }

    @Test
    void rejectsMissingOrWrongToken() {
        var request = new AgentPriceRefreshController.RefreshRequest(1, "SEPA_AMBA", true);
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
                () -> controller.requestRefresh(null, request)).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
                () -> controller.requestRefresh("wrong", request)).getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnconfiguredIntegration() {
        ReflectionTestUtils.setField(controller, "apiToken", "");
        var request = new AgentPriceRefreshController.RefreshRequest(1, "SEPA_AMBA", true);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> controller.requestRefresh("anything", request)).getStatusCode());
    }

    @Test
    void acceptsValidTokenAndDelegatesToService() {
        var request = new AgentPriceRefreshController.RefreshRequest(7, "SEPA_AMBA", null);
        var expected = new AgentPriceRefreshController.RefreshAccepted(
                42, ScrapingJob.Status.PENDING, true);
        when(service.start(7, "SEPA_AMBA", null)).thenReturn(expected);

        var response = controller.requestRefresh("test-secret-not-for-production", request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(42, response.getBody().jobId());
        verify(service).start(7, "SEPA_AMBA", null);
    }
}
