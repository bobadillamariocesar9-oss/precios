package com.apiprecios.service;

import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.entity.ScrapingJob.Status;
import com.apiprecios.entity.Store;
import com.apiprecios.exception.BadRequestException;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.ScrapingJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScrapingJobService")
class ScrapingJobServiceTest {

    @Mock private ScrapingJobRepository scrapingJobRepository;
    @Mock private StoreService storeService;

    @InjectMocks
    private ScrapingJobService scrapingJobService;

    private Store mercado;
    private ScrapingJob pendingJob;
    private ScrapingJob runningJob;
    private ScrapingJob completedJob;

    @BeforeEach
    void setUp() {
        mercado = Store.builder().name("MercadoLibre").url("https://ml.com").build();
        mercado.setId(1);

        pendingJob = ScrapingJob.builder()
                .store(mercado).url("https://ml.com/electronica")
                .status(Status.PENDING).build();

        runningJob = ScrapingJob.builder()
                .store(mercado).url("https://ml.com/computacion")
                .status(Status.RUNNING).build();

        completedJob = ScrapingJob.builder()
                .store(mercado).url("https://ml.com/celulares")
                .status(Status.COMPLETED).build();
    }

    // ─── findAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: delega al repositorio")
    void findAll_delegatesToRepository() {
        when(scrapingJobRepository.findAll())
                .thenReturn(List.of(pendingJob, runningJob, completedJob));

        assertThat(scrapingJobService.findAll()).hasSize(3);
        verify(scrapingJobRepository).findAll();
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: retorna job cuando existe")
    void findById_returnsJobWhenFound() {
        when(scrapingJobRepository.findById(1)).thenReturn(Optional.of(pendingJob));

        assertThat(scrapingJobService.findById(1)).isEqualTo(pendingJob);
    }

    @Test
    @DisplayName("findById: lanza ResourceNotFoundException cuando no existe")
    void findById_throwsWhenNotFound() {
        when(scrapingJobRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scrapingJobService.findById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── findByStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatus: retorna jobs con el status indicado")
    void findByStatus_returnsFilteredJobs() {
        when(scrapingJobRepository.findByStatus(Status.PENDING))
                .thenReturn(List.of(pendingJob));

        List<ScrapingJob> result = scrapingJobService.findByStatus(Status.PENDING);

        assertThat(result).containsExactly(pendingJob);
        verify(scrapingJobRepository).findByStatus(Status.PENDING);
    }

    // ─── findByStore ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStore: valida tienda y retorna sus jobs")
    void findByStore_validatesStoreAndReturnsJobs() {
        when(storeService.findById(1)).thenReturn(mercado);
        when(scrapingJobRepository.findByStoreId(1))
                .thenReturn(List.of(pendingJob, completedJob));

        List<ScrapingJob> result = scrapingJobService.findByStore(1);

        assertThat(result).hasSize(2);
        verify(storeService).findById(1);
    }

    @Test
    @DisplayName("findByStore: lanza excepción si la tienda no existe")
    void findByStore_throwsWhenStoreNotFound() {
        when(storeService.findById(99))
                .thenThrow(new ResourceNotFoundException("Store", 99));

        assertThatThrownBy(() -> scrapingJobService.findByStore(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(scrapingJobRepository, never()).findByStoreId(any());
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: valida tienda, fuerza status PENDING y guarda")
    void create_setsStatusPendingAndSaves() {
        ScrapingJob newJob = ScrapingJob.builder()
                .store(mercado)
                .url("https://ml.com/nuevo")
                .status(Status.RUNNING)   // el servicio debe sobreescribir a PENDING
                .build();

        when(storeService.findById(1)).thenReturn(mercado);
        when(scrapingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingJob result = scrapingJobService.create(newJob);

        assertThat(result.getStatus()).isEqualTo(Status.PENDING);
        verify(scrapingJobRepository).save(newJob);
    }

    @Test
    @DisplayName("create: lanza excepción si la tienda no existe")
    void create_throwsWhenStoreNotFound() {
        mercado.setId(99);
        when(storeService.findById(99))
                .thenThrow(new ResourceNotFoundException("Store", 99));

        assertThatThrownBy(() -> scrapingJobService.create(pendingJob))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(scrapingJobRepository, never()).save(any());
    }

    // ─── updateStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: PENDING→RUNNING asigna startedAt")
    void updateStatus_pendingToRunning_setsStartedAt() {
        when(scrapingJobRepository.findById(1)).thenReturn(Optional.of(pendingJob));
        when(scrapingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingJob result = scrapingJobService.updateStatus(1, Status.RUNNING);

        assertThat(result.getStatus()).isEqualTo(Status.RUNNING);
        assertThat(result.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus: RUNNING→COMPLETED asigna finishedAt")
    void updateStatus_runningToCompleted_setsFinishedAt() {
        when(scrapingJobRepository.findById(1)).thenReturn(Optional.of(runningJob));
        when(scrapingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingJob result = scrapingJobService.updateStatus(1, Status.COMPLETED);

        assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(result.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus: RUNNING→FAILED asigna finishedAt")
    void updateStatus_runningToFailed_setsFinishedAt() {
        when(scrapingJobRepository.findById(1)).thenReturn(Optional.of(runningJob));
        when(scrapingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingJob result = scrapingJobService.updateStatus(1, Status.FAILED);

        assertThat(result.getStatus()).isEqualTo(Status.FAILED);
        assertThat(result.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus: lanza BadRequestException si job ya está COMPLETED")
    void updateStatus_throwsWhenAlreadyCompleted() {
        when(scrapingJobRepository.findById(1)).thenReturn(Optional.of(completedJob));

        assertThatThrownBy(() -> scrapingJobService.updateStatus(1, Status.RUNNING))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COMPLETED");

        verify(scrapingJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: lanza BadRequestException si job ya está FAILED")
    void updateStatus_throwsWhenAlreadyFailed() {
        ScrapingJob failedJob = ScrapingJob.builder()
                .store(mercado).status(Status.FAILED).build();
        when(scrapingJobRepository.findById(1)).thenReturn(Optional.of(failedJob));

        assertThatThrownBy(() -> scrapingJobService.updateStatus(1, Status.PENDING))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    @DisplayName("updateStatus: lanza ResourceNotFoundException si job no existe")
    void updateStatus_throwsWhenNotFound() {
        when(scrapingJobRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scrapingJobService.updateStatus(99, Status.RUNNING))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina job existente")
    void delete_removesJob() {
        when(scrapingJobRepository.findById(1)).thenReturn(Optional.of(pendingJob));

        scrapingJobService.delete(1);

        verify(scrapingJobRepository).delete(pendingJob);
    }

    @Test
    @DisplayName("delete: lanza ResourceNotFoundException si no existe")
    void delete_throwsWhenNotFound() {
        when(scrapingJobRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scrapingJobService.delete(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(scrapingJobRepository, never()).delete(any());
    }
}
