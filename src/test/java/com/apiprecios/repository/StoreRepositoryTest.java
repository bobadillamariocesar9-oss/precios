package com.apiprecios.repository;

import com.apiprecios.AbstractRepositoryTest;
import com.apiprecios.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StoreRepository")
class StoreRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    private Store mercado;
    private Store falabella;

    @BeforeEach
    void setUp() {
        storeRepository.deleteAll();

        mercado = storeRepository.save(Store.builder()
                .name("MercadoLibre")
                .url("https://www.mercadolibre.com")
                .logoUrl("https://cdn.mercadolibre.com/logo.png")
                .build());

        falabella = storeRepository.save(Store.builder()
                .name("Falabella")
                .url("https://www.falabella.com")
                .logoUrl("https://cdn.falabella.com/logo.png")
                .build());

        storeRepository.save(Store.builder()
                .name("Mercado Pago")
                .url("https://www.mercadopago.com")
                .build());
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save: persiste una tienda y asigna ID")
    void save_persistsStoreWithId() {
        Store store = storeRepository.save(Store.builder()
                .name("Ripley")
                .url("https://www.ripley.com")
                .build());

        assertThat(store.getId()).isNotNull();
        assertThat(store.getCreatedAt()).isNotNull();
    }

    // ─── findAll ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: retorna todas las tiendas")
    void findAll_returnsAllStores() {
        assertThat(storeRepository.findAll()).hasSize(3);
    }

    // ─── findById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: retorna tienda existente")
    void findById_returnsStore() {
        Optional<Store> found = storeRepository.findById(mercado.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUrl()).isEqualTo("https://www.mercadolibre.com");
    }

    @Test
    @DisplayName("findById: retorna vacío para ID inexistente")
    void findById_returnsEmptyForUnknownId() {
        assertThat(storeRepository.findById(99999)).isEmpty();
    }

    // ─── findByName ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByName: retorna tienda por nombre exacto")
    void findByName_returnsStore() {
        Optional<Store> found = storeRepository.findByName("Falabella");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(falabella.getId());
    }

    @Test
    @DisplayName("findByName: retorna vacío para nombre inexistente")
    void findByName_returnsEmptyForUnknownName() {
        assertThat(storeRepository.findByName("TiendaFantasma")).isEmpty();
    }

    @Test
    @DisplayName("findByName: distingue mayúsculas (búsqueda exacta)")
    void findByName_isCaseSensitive() {
        // findByName es exacto; "falabella" en minúscula no coincide con "Falabella"
        assertThat(storeRepository.findByName("falabella")).isEmpty();
    }

    // ─── findByNameContainingIgnoreCase ───────────────────────────────────────

    @Test
    @DisplayName("findByNameContainingIgnoreCase: retorna coincidencias parciales")
    void findByNameContaining_returnsPartialMatches() {
        List<Store> result = storeRepository.findByNameContainingIgnoreCase("mercado");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Store::getName)
                .containsExactlyInAnyOrder("MercadoLibre", "Mercado Pago");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase: es insensible a mayúsculas")
    void findByNameContaining_isCaseInsensitive() {
        List<Store> result = storeRepository.findByNameContainingIgnoreCase("FALABELLA");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Falabella");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase: retorna lista vacía si no hay coincidencias")
    void findByNameContaining_returnsEmptyWhenNoMatch() {
        List<Store> result = storeRepository.findByNameContainingIgnoreCase("noexiste");
        assertThat(result).isEmpty();
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina una tienda")
    void delete_removesStore() {
        storeRepository.delete(falabella);
        assertThat(storeRepository.findAll()).hasSize(2);
        assertThat(storeRepository.findById(falabella.getId())).isEmpty();
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: modifica la URL de una tienda")
    void update_changesUrl() {
        mercado.setUrl("https://www.mercadolibre.com.ar");
        Store saved = storeRepository.save(mercado);
        assertThat(saved.getUrl()).isEqualTo("https://www.mercadolibre.com.ar");
    }
}
