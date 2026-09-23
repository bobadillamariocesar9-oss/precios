package com.apiprecios.repository;

import com.apiprecios.AbstractRepositoryTest;
import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.entity.ScrapingJob.Status;
import com.apiprecios.entity.ScrapingLog;
import com.apiprecios.entity.ScrapingLog.Level;
import com.apiprecios.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ScrapingLogRepository")
class ScrapingLogRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ScrapingLogRepository scrapingLogRepository;

    @Autowired
    private ScrapingJobRepository scrapingJobRepository;

    @Autowired
    private StoreRepository storeRepository;

    private ScrapingJob jobA;
    private ScrapingJob jobB;

    @BeforeEach
    void setUp() throws InterruptedException {
        scrapingLogRepository.deleteAll();
        scrapingJobRepository.deleteAll();
        storeRepository.deleteAll();

        Store store = storeRepository.save(Store.builder()
                .name("MercadoLibre").url("https://ml.com").build());

        jobA = scrapingJobRepository.save(ScrapingJob.builder()
                .store(store).url("https://ml.com/electronica")
                .status(Status.COMPLETED).build());
        jobB = scrapingJobRepository.save(ScrapingJob.builder()
                .store(store).url("https://ml.com/hogar")
                .status(Status.FAILED).build());

        // jobA: INFO → WARN → ERROR (en orden temporal ascendente)
        LocalDateTime base = LocalDateTime.now().minusSeconds(10);

        scrapingLogRepository.save(ScrapingLog.builder()
                .scrapingJob(jobA).level(Level.INFO)
                .message("Inicio del scraping")
                .createdAt(base)
                .build());
        scrapingLogRepository.save(ScrapingLog.builder()
                .scrapingJob(jobA).level(Level.WARN)
                .message("Respuesta lenta del servidor")
                .createdAt(base.plusSeconds(3))
                .build());
        scrapingLogRepository.save(ScrapingLog.builder()
                .scrapingJob(jobA).level(Level.ERROR)
                .message("Timeout al obtener precio del producto 42")
                .createdAt(base.plusSeconds(7))
                .build());
        scrapingLogRepository.save(ScrapingLog.builder()
                .scrapingJob(jobA).level(Level.INFO)
                .message("Scraping finalizado con advertencias")
                .createdAt(base.plusSeconds(9))
                .build());

        // jobB: solo ERROR
        scrapingLogRepository.save(ScrapingLog.builder()
                .scrapingJob(jobB).level(Level.ERROR)
                .message("Conexión rechazada por el servidor")
                .build());
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save: persiste log con level INFO por defecto y createdAt auto-asignado")
    void save_persistsLogWithDefaultsAndCreatedAt() {
        ScrapingLog log = scrapingLogRepository.save(ScrapingLog.builder()
                .scrapingJob(jobA)
                .message("Log adicional")
                .build());

        assertThat(log.getId()).isNotNull();
        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getCreatedAt()).isNotNull();
    }

    // ─── findAll ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: retorna todos los logs")
    void findAll_returnsAllLogs() {
        assertThat(scrapingLogRepository.findAll()).hasSize(5);
    }

    // ─── findByScrapingJobId ──────────────────────────────────────────────────

    @Test
    @DisplayName("findByScrapingJobId: retorna todos los logs de un job")
    void findByScrapingJobId_returnsAllLogsForJob() {
        List<ScrapingLog> logs = scrapingLogRepository
                .findByScrapingJobId(jobA.getId());
        assertThat(logs).hasSize(4);
    }

    @Test
    @DisplayName("findByScrapingJobId: retorna solo los logs del job indicado")
    void findByScrapingJobId_returnsOnlyLogsOfThatJob() {
        List<ScrapingLog> logs = scrapingLogRepository
                .findByScrapingJobId(jobB.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getMessage()).contains("rechazada");
    }

    @Test
    @DisplayName("findByScrapingJobId: retorna vacío para job sin logs")
    void findByScrapingJobId_returnsEmptyForJobWithNoLogs() {
        Store store = storeRepository.findAll().get(0);
        ScrapingJob jobSinLogs = scrapingJobRepository.save(ScrapingJob.builder()
                .store(store).url("https://ml.com/sinlogs")
                .status(Status.PENDING).build());
        assertThat(scrapingLogRepository.findByScrapingJobId(jobSinLogs.getId())).isEmpty();
    }

    // ─── findByScrapingJobIdAndLevel ──────────────────────────────────────────

    @Test
    @DisplayName("findByScrapingJobIdAndLevel: filtra por job y level INFO")
    void findByScrapingJobIdAndLevel_filtersByInfo() {
        List<ScrapingLog> infos = scrapingLogRepository
                .findByScrapingJobIdAndLevel(jobA.getId(), Level.INFO);
        assertThat(infos).hasSize(2);
        assertThat(infos).allMatch(l -> l.getLevel() == Level.INFO);
    }

    @Test
    @DisplayName("findByScrapingJobIdAndLevel: filtra por job y level WARN")
    void findByScrapingJobIdAndLevel_filtersByWarn() {
        List<ScrapingLog> warns = scrapingLogRepository
                .findByScrapingJobIdAndLevel(jobA.getId(), Level.WARN);
        assertThat(warns).hasSize(1);
        assertThat(warns.get(0).getMessage()).contains("Respuesta lenta");
    }

    @Test
    @DisplayName("findByScrapingJobIdAndLevel: filtra por job y level ERROR")
    void findByScrapingJobIdAndLevel_filtersByError() {
        List<ScrapingLog> errors = scrapingLogRepository
                .findByScrapingJobIdAndLevel(jobA.getId(), Level.ERROR);
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getMessage()).contains("Timeout");
    }

    @Test
    @DisplayName("findByScrapingJobIdAndLevel: retorna vacío cuando no hay logs con ese level")
    void findByScrapingJobIdAndLevel_returnsEmptyWhenNoMatch() {
        List<ScrapingLog> debugLogs = scrapingLogRepository
                .findByScrapingJobIdAndLevel(jobA.getId(), Level.DEBUG);
        assertThat(debugLogs).isEmpty();
    }

    @Test
    @DisplayName("findByScrapingJobIdAndLevel: no mezcla logs de otros jobs")
    void findByScrapingJobIdAndLevel_doesNotMixJobLogs() {
        // jobB tiene 1 ERROR; jobA tiene 1 ERROR — deben estar separados
        List<ScrapingLog> errorsA = scrapingLogRepository
                .findByScrapingJobIdAndLevel(jobA.getId(), Level.ERROR);
        List<ScrapingLog> errorsB = scrapingLogRepository
                .findByScrapingJobIdAndLevel(jobB.getId(), Level.ERROR);

        assertThat(errorsA).hasSize(1);
        assertThat(errorsB).hasSize(1);
        assertThat(errorsA.get(0).getId()).isNotEqualTo(errorsB.get(0).getId());
    }

    // ─── findByScrapingJobIdOrderByCreatedAtDesc ──────────────────────────────

    @Test
    @DisplayName("findByScrapingJobIdOrderByCreatedAtDesc: retorna logs más reciente primero")
    void findByScrapingJobIdOrderByCreatedAtDesc_returnsNewestFirst() {
        List<ScrapingLog> logs = scrapingLogRepository
                .findByScrapingJobIdOrderByCreatedAtDesc(jobA.getId());

        assertThat(logs).hasSize(4);
        // El más reciente debe ser "Scraping finalizado con advertencias"
        assertThat(logs.get(0).getMessage()).contains("finalizado");
        // El más antiguo al final: "Inicio del scraping"
        assertThat(logs.get(logs.size() - 1).getMessage()).contains("Inicio");
    }

    @Test
    @DisplayName("findByScrapingJobIdOrderByCreatedAtDesc: orden descendente estricto")
    void findByScrapingJobIdOrderByCreatedAtDesc_isStrictlyDescending() {
        List<ScrapingLog> logs = scrapingLogRepository
                .findByScrapingJobIdOrderByCreatedAtDesc(jobA.getId());

        for (int i = 0; i < logs.size() - 1; i++) {
            assertThat(logs.get(i).getCreatedAt())
                    .isAfterOrEqualTo(logs.get(i + 1).getCreatedAt());
        }
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina un log sin afectar el job")
    void delete_removesLogWithoutAffectingJob() {
        ScrapingLog toDelete = scrapingLogRepository
                .findByScrapingJobId(jobA.getId()).get(0);
        scrapingLogRepository.delete(toDelete);

        assertThat(scrapingLogRepository.findByScrapingJobId(jobA.getId())).hasSize(3);
        assertThat(scrapingJobRepository.findById(jobA.getId())).isPresent();
    }
}
