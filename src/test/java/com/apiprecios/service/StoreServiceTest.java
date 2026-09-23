package com.apiprecios.service;

import com.apiprecios.entity.Store;
import com.apiprecios.exception.BadRequestException;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.StoreRepository;
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
@DisplayName("StoreService")
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreService storeService;

    private Store mercado;

    @BeforeEach
    void setUp() {
        mercado = Store.builder()
                .name("MercadoLibre")
                .baseUrl("https://ml.com")
                .logoUrl("https://ml.com/logo.png")
                .build();
    }

    // ─── findAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: retorna la lista del repositorio")
    void findAll_returnsRepositoryList() {
        when(storeRepository.findAll()).thenReturn(List.of(mercado));

        List<Store> result = storeService.findAll();

        assertThat(result).containsExactly(mercado);
        verify(storeRepository).findAll();
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: retorna tienda cuando existe")
    void findById_returnsStoreWhenFound() {
        when(storeRepository.findById(1)).thenReturn(Optional.of(mercado));

        Store result = storeService.findById(1);

        assertThat(result).isEqualTo(mercado);
    }

    @Test
    @DisplayName("findById: lanza ResourceNotFoundException cuando no existe")
    void findById_throwsWhenNotFound() {
        when(storeRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.findById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── search ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search: delega a findByNameContainingIgnoreCase")
    void search_delegatesToRepository() {
        when(storeRepository.findByNameContainingIgnoreCase("mercado"))
                .thenReturn(List.of(mercado));

        List<Store> result = storeService.search("mercado");

        assertThat(result).containsExactly(mercado);
        verify(storeRepository).findByNameContainingIgnoreCase("mercado");
    }

    @Test
    @DisplayName("search: retorna lista vacía si no hay coincidencias")
    void search_returnsEmptyWhenNoMatches() {
        when(storeRepository.findByNameContainingIgnoreCase("xyz")).thenReturn(List.of());

        assertThat(storeService.search("xyz")).isEmpty();
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: guarda tienda cuando el nombre es único")
    void create_savesStoreWhenNameIsUnique() {
        when(storeRepository.findByName("MercadoLibre")).thenReturn(Optional.empty());
        when(storeRepository.save(mercado)).thenReturn(mercado);

        Store result = storeService.create(mercado);

        assertThat(result).isEqualTo(mercado);
        verify(storeRepository).save(mercado);
    }

    @Test
    @DisplayName("create: lanza BadRequestException cuando el nombre ya existe")
    void create_throwsWhenNameDuplicated() {
        when(storeRepository.findByName("MercadoLibre")).thenReturn(Optional.of(mercado));

        assertThatThrownBy(() -> storeService.create(mercado))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("MercadoLibre");

        verify(storeRepository, never()).save(any());
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: modifica campos y guarda")
    void update_updatesAndSaves() {
        Store data = Store.builder()
                .name("MercadoLibre AR")
                .baseUrl("https://www.mercadolibre.com.ar")
                .logoUrl("https://ml.com/logo-ar.png")
                .build();

        when(storeRepository.findById(1)).thenReturn(Optional.of(mercado));
        when(storeRepository.findByName("MercadoLibre AR")).thenReturn(Optional.empty());
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store result = storeService.update(1, data);

        assertThat(result.getName()).isEqualTo("MercadoLibre AR");
        assertThat(result.getBaseUrl()).isEqualTo("https://www.mercadolibre.com.ar");
    }

    @Test
    @DisplayName("update: no valida nombre si no cambia")
    void update_skipsNameValidationWhenUnchanged() {
        Store data = Store.builder()
                .name("MercadoLibre")    // mismo nombre
                .baseUrl("https://ml.com.ar")
                .build();

        when(storeRepository.findById(1)).thenReturn(Optional.of(mercado));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        storeService.update(1, data);

        verify(storeRepository, never()).findByName("MercadoLibre");
    }

    @Test
    @DisplayName("update: lanza BadRequestException si nuevo nombre ya existe")
    void update_throwsWhenNewNameConflicts() {
        Store data = Store.builder().name("Falabella").baseUrl("https://falabella.com").build();
        Store falabella = Store.builder().name("Falabella").build();

        when(storeRepository.findById(1)).thenReturn(Optional.of(mercado));
        when(storeRepository.findByName("Falabella")).thenReturn(Optional.of(falabella));

        assertThatThrownBy(() -> storeService.update(1, data))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Falabella");
    }

    @Test
    @DisplayName("update: lanza ResourceNotFoundException si tienda no existe")
    void update_throwsWhenStoreNotFound() {
        when(storeRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.update(99, mercado))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina tienda existente")
    void delete_removesStore() {
        when(storeRepository.findById(1)).thenReturn(Optional.of(mercado));

        storeService.delete(1);

        verify(storeRepository).delete(mercado);
    }

    @Test
    @DisplayName("delete: lanza ResourceNotFoundException si no existe")
    void delete_throwsWhenNotFound() {
        when(storeRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.delete(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(storeRepository, never()).delete(any());
    }
}
