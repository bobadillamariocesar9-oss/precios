package com.apiprecios.repository;

import com.apiprecios.AbstractRepositoryTest;
import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.entity.ScrapingJob.Status;
import com.apiprecios.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ScrapingJobRepository")
class ScrapingJobRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ScrapingJobRepository scrapingJobRepository;

    @Autowired
    private StoreRepository storeRepository;

    private Store mercado;
    private Store falabella;

    @BeforeEach
    void setUp() {
        scrapingJobRepository.deleteAll();
        storeRepository.deleteAll();

        mercado = storeRepository.save(Store.builder()
                .name("MercadoLibre").baseUrl("https://ml.com").build());
        falabella = storeRepository.save(Store.builder()
                .name("Falabella").baseUrl("https://falabella.com").build());

        // MercadoLibre: 1 PENDING, 1 RUNNING, 1 COMPLETED
        scrapingJobRepository.save(ScrapingJob.builder()
                .store(mercado)
                .baseUrl("https://ml.com/electronica")
                .status(Status.PENDING)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .build());
        scrapingJobRepository.save(ScrapingJob.builder()
                .store(mercado)
                .baseUrl("https://ml.com/computacion")
                .status(Status.RUNNING)
                .startedAt(LocalDateTime.now())
                .build());
        scrapingJobRepository.save(ScrapingJob.builder()
                .store(mercado)
                .baseUrl("https://ml.com/celulares")
                .status(Status.COMPLETED)
                .startedAt(LocalDateTime.now().minusHours(2))
                .finishedAt(LocalDateTime.now().minusHours(1))
                .build());

        // Falabella: 1 PENDING, 1 FAILED
        scrapingJobRepository.save(ScrapingJob.builder()
                .store(falabella)
                .baseUrl("https://falabella.com/tecnologia")
                .status(Status.PENDING)
                .scheduledAt(LocalDateTime.now().plusHours(2))
                .build());
        scrapingJobRepository.save(ScrapingJob.builder()
                .store(falabella)
                .baseUrl("https://falabella.com/hogar")
                .status(Status.FAILED)
                .startedAt(LocalDateTime.now().minusHours(3))
                .finishedAt(LocalDateTime.now().minusHours(3).plusMinutes(5))
                .build());
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save: persiste job con status PENDING por defecto")
    void save_persistsJobWithDefaultPendingStatus() {
        ScrapingJob job = scrapingJobRepository.save(ScrapingJob.builder()
                .store(mercado)
                .baseUrl("https://ml.com/nuevo")
                .build());

        assertThat(job.getId()).isNotNull();
        assertThat(job.getStatus()).isEqualTo(Status.PENDING);
        assertThat(job.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("save: persiste job con status explícito")
    void save_persistsJobWithExplicitStatus() {
        ScrapingJob job = scrapingJobRepository.save(ScrapingJob.builder()
                .store(falabella)
                .baseUrl("https://falabella.com/nuevo")
                .status(Status.RUNNING)
                .build());

        assertThat(job.getStatus()).isEqualTo(Status.RUNNING);
    }

    // ─── findAll ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: retorna todos los jobs")
    void findAll_returnsAllJobs() {
        assertThat(scrapingJobRepository.findAll()).hasSize(5);
    }

    // ─── findByStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatus: retorna todos los jobs PENDING")
    void findByStatus_returnsPendingJobs() {
        List<ScrapingJob> pending = scrapingJobRepository.findByStatus(Status.PENDING);
        assertThat(pending).hasSize(2);
        assertThat(pending).allMatch(j -> j.getStatus() == Status.PENDING);
    }

    @Test
    @DisplayName("findByStatus: retorna job RUNNING")
    void findByStatus_returnsRunningJob() {
        List<ScrapingJob> running = scrapingJobRepository.findByStatus(Status.RUNNING);
        assertThat(running).hasSize(1);
        assertThat(running.get(0).getBaseUrl()).isEqualTo("https://ml.com/computacion");
    }

    @Test
    @DisplayName("findByStatus: retorna job COMPLETED")
    void findByStatus_returnsCompletedJob() {
        List<ScrapingJob> completed = scrapingJobRepository.findByStatus(Status.COMPLETED);
        assertThat(completed).hasSize(1);
    }

    @Test
    @DisplayName("findByStatus: retorna job FAILED")
    void findByStatus_returnsFailedJob() {
        List<ScrapingJob> failed = scrapingJobRepository.findByStatus(Status.FAILED);
        assertThat(failed).hasSize(1);
        assertThat(failed.get(0).getStore().getId()).isEqualTo(falabella.getId());
    }

    @Test
    @DisplayName("findByStatus: retorna vacío cuando no hay jobs con ese status")
    void findByStatus_returnsEmptyWhenNoJobsWithStatus() {
        // Eliminamos los RUNNING para probar vacío
        scrapingJobRepository.findByStatus(Status.RUNNING)
                .forEach(scrapingJobRepository::delete);
        assertThat(scrapingJobRepository.findByStatus(Status.RUNNING)).isEmpty();
    }

    // ─── findByStoreId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStoreId: retorna todos los jobs de la tienda indicada")
    void findByStoreId_returnsJobsForStore() {
        List<ScrapingJob> jobs = scrapingJobRepository.findByStoreId(mercado.getId());
        assertThat(jobs).hasSize(3);
        assertThat(jobs).allMatch(j -> j.getStore().getId().equals(mercado.getId()));
    }

    @Test
    @DisplayName("findByStoreId: retorna vacío si tienda sin jobs")
    void findByStoreId_returnsEmptyForStoreWithNoJobs() {
        Store nueva = storeRepository.save(Store.builder()
                .name("Ripley").baseUrl("https://ripley.com").build());
        assertThat(scrapingJobRepository.findByStoreId(nueva.getId())).isEmpty();
    }

    // ─── findByStoreIdAndStatus ───────────────────────────────────────────────

    @Test
    @DisplayName("findByStoreIdAndStatus: filtra por tienda y status")
    void findByStoreIdAndStatus_returnsFilteredJobs() {
        List<ScrapingJob> result = scrapingJobRepository
                .findByStoreIdAndStatus(mercado.getId(), Status.PENDING);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBaseUrl()).isEqualTo("https://ml.com/electronica");
    }

    @Test
    @DisplayName("findByStoreIdAndStatus: retorna vacío cuando no hay coincidencia")
    void findByStoreIdAndStatus_returnsEmptyWhenNoMatch() {
        List<ScrapingJob> result = scrapingJobRepository
                .findByStoreIdAndStatus(falabella.getId(), Status.RUNNING);
        assertThat(result).isEmpty();
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: cambia status de PENDING a RUNNING")
    void update_changesStatusFromPendingToRunning() {
        ScrapingJob job = scrapingJobRepository
                .findByStatus(Status.PENDING).get(0);
        job.setStatus(Status.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        ScrapingJob saved = scrapingJobRepository.save(job);

        assertThat(saved.getStatus()).isEqualTo(Status.RUNNING);
        assertThat(saved.getStartedAt()).isNotNull();
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina un job")
    void delete_removesJob() {
        long before = scrapingJobRepository.count();
        ScrapingJob toDelete = scrapingJobRepository.findByStatus(Status.FAILED).get(0);
        scrapingJobRepository.delete(toDelete);
        assertThat(scrapingJobRepository.count()).isEqualTo(before - 1);
    }
}
