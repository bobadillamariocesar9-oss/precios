package com.apiprecios.service;

import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.entity.ScrapingJob.Status;
import com.apiprecios.entity.ScrapingLog;
import com.apiprecios.entity.ScrapingLog.Level;
import com.apiprecios.entity.Store;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.ScrapingLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScrapingLogService")
class ScrapingLogServiceTest {

    @Mock private ScrapingLogRepository scrapingLogRepository;
    @Mock private ScrapingJobService scrapingJobService;

    @InjectMocks
    private ScrapingLogService scrapingLogService;

    private ScrapingJob job;
    private ScrapingLog infoLog;
    private ScrapingLog errorLog;

    @BeforeEach
    void setUp() {
        Store store = Store.builder().name("MercadoLibre").baseUrl("https://ml.com").build();
        store.setId(1);

        job = ScrapingJob.builder()
                .store(store).baseUrl("https://ml.com/electronica")
                .status(Status.RUNNING).build();
        job.setId(1);

        infoLog = ScrapingLog.builder()
                .scrapingJob(job).message("Inicio del scraping").level(Level.INFO).build();
        errorLog = ScrapingLog.builder()
                .scrapingJob(job).message("Error en conexión").level(Level.ERROR).build();
    }

    // ─── findByJob ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByJob: valida job y retorna logs ordenados desc")
    void findByJob_validatesJobAndReturnsLogs() {
        when(scrapingJobService.findById(1)).thenReturn(job);
        when(scrapingLogRepository.findByScrapingJobIdOrderByCreatedAtDesc(1))
                .thenReturn(List.of(errorLog, infoLog));

        List<ScrapingLog> result = scrapingLogService.findByJob(1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLevel()).isEqualTo(Level.ERROR);
        verify(scrapingJobService).findById(1);
    }

    @Test
    @DisplayName("findByJob: lanza excepción si el job no existe")
    void findByJob_throwsWhenJobNotFound() {
        when(scrapingJobService.findById(99))
                .thenThrow(new ResourceNotFoundException("ScrapingJob", 99));

        assertThatThrownBy(() -> scrapingLogService.findByJob(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(scrapingLogRepository, never()).findByScrapingJobIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("findByJob: retorna lista vacía si job sin logs")
    void findByJob_returnsEmptyWhenNoLogs() {
        when(scrapingJobService.findById(1)).thenReturn(job);
        when(scrapingLogRepository.findByScrapingJobIdOrderByCreatedAtDesc(1))
                .thenReturn(List.of());

        assertThat(scrapingLogService.findByJob(1)).isEmpty();
    }

    // ─── findByJobAndLevel ────────────────────────────────────────────────────

    @Test
    @DisplayName("findByJobAndLevel: valida job y filtra por level")
    void findByJobAndLevel_validatesAndFilters() {
        when(scrapingJobService.findById(1)).thenReturn(job);
        when(scrapingLogRepository.findByScrapingJobIdAndLevel(1, Level.ERROR))
                .thenReturn(List.of(errorLog));

        List<ScrapingLog> result = scrapingLogService.findByJobAndLevel(1, Level.ERROR);

        assertThat(result).containsExactly(errorLog);
        verify(scrapingLogRepository).findByScrapingJobIdAndLevel(1, Level.ERROR);
    }

    @Test
    @DisplayName("findByJobAndLevel: retorna vacío cuando no hay logs con ese level")
    void findByJobAndLevel_returnsEmptyWhenNoMatch() {
        when(scrapingJobService.findById(1)).thenReturn(job);
        when(scrapingLogRepository.findByScrapingJobIdAndLevel(1, Level.DEBUG))
                .thenReturn(List.of());

        assertThat(scrapingLogService.findByJobAndLevel(1, Level.DEBUG)).isEmpty();
    }

    // ─── log ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("log: crea log con el level y mensaje indicados")
    void log_createsLogWithSpecifiedLevel() {
        when(scrapingJobService.findById(1)).thenReturn(job);
        when(scrapingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingLog result = scrapingLogService.log(1, "Mensaje de prueba", Level.WARN);

        assertThat(result.getMessage()).isEqualTo("Mensaje de prueba");
        assertThat(result.getLevel()).isEqualTo(Level.WARN);
        assertThat(result.getScrapingJob()).isEqualTo(job);
    }

    @Test
    @DisplayName("log: usa INFO cuando level es null")
    void log_usesInfoWhenLevelIsNull() {
        when(scrapingJobService.findById(1)).thenReturn(job);
        when(scrapingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingLog result = scrapingLogService.log(1, "Sin level", null);

        assertThat(result.getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    @DisplayName("log: lanza excepción si el job no existe")
    void log_throwsWhenJobNotFound() {
        when(scrapingJobService.findById(99))
                .thenThrow(new ResourceNotFoundException("ScrapingJob", 99));

        assertThatThrownBy(() -> scrapingLogService.log(99, "msg", Level.INFO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(scrapingLogRepository, never()).save(any());
    }

    // ─── info / warn / error (helpers) ───────────────────────────────────────

    @Test
    @DisplayName("info: crea log con level INFO")
    void info_createsInfoLog() {
        when(scrapingJobService.findById(1)).thenReturn(job);
        when(scrapingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingLog result = scrapingLogService.info(1, "Proceso iniciado");

        assertThat(result.getLevel()).isEqualTo(Level.INFO);
        assertThat(result.getMessage()).isEqualTo("Proceso iniciado");
    }

    @Test
    @DisplayName("warn: crea log con level WARN")
    void warn_createsWarnLog() {
        when(scrapingJobService.findById(1)).thenReturn(job);
        when(scrapingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingLog result = scrapingLogService.warn(1, "Respuesta lenta");

        assertThat(result.getLevel()).isEqualTo(Level.WARN);
    }

    @Test
    @DisplayName("error: crea log con level ERROR")
    void error_createsErrorLog() {
        when(scrapingJobService.findById(1)).thenReturn(job);
        when(scrapingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingLog result = scrapingLogService.error(1, "Timeout al conectar");

        assertThat(result.getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    @DisplayName("info/warn/error: todos persisten exactamente un log por llamada")
    void helpers_persistExactlyOneLogEach() {
        when(scrapingJobService.findById(1)).thenReturn(job);
        when(scrapingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scrapingLogService.info(1, "a");
        scrapingLogService.warn(1, "b");
        scrapingLogService.error(1, "c");

        verify(scrapingLogRepository, times(3)).save(any());
    }

    @Test
    @DisplayName("info/warn/error: usan el mensaje correcto")
    void helpers_useCorrectMessages() {
        when(scrapingJobService.findById(1)).thenReturn(job);

        ArgumentCaptor<ScrapingLog> captor = ArgumentCaptor.forClass(ScrapingLog.class);
        when(scrapingLogRepository.save(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        scrapingLogService.info(1, "info msg");
        scrapingLogService.warn(1, "warn msg");
        scrapingLogService.error(1, "error msg");

        List<ScrapingLog> captured = captor.getAllValues();
        assertThat(captured).extracting(ScrapingLog::getMessage)
                .containsExactly("info msg", "warn msg", "error msg");
        assertThat(captured).extracting(ScrapingLog::getLevel)
                .containsExactly(Level.INFO, Level.WARN, Level.ERROR);
    }
}
