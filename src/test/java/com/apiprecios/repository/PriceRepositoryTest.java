package com.apiprecios.repository;

import com.apiprecios.AbstractRepositoryTest;
import com.apiprecios.entity.Price;
import com.apiprecios.entity.Product;
import com.apiprecios.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PriceRepository")
class PriceRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    private Store mercado;
    private Store falabella;
    private Product notebook;
    private Product mouse;

    private final LocalDateTime BASE = LocalDateTime.of(2025, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        priceRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();

        mercado = storeRepository.save(Store.builder()
                .name("MercadoLibre").baseUrl("https://www.mercadolibre.com").build());
        falabella = storeRepository.save(Store.builder()
                .name("Falabella").baseUrl("https://www.falabella.com").build());

        notebook = productRepository.save(Product.builder()
                .name("Notebook Dell").store(mercado).build());
        mouse = productRepository.save(Product.builder()
                .name("Mouse Logitech").store(mercado).build());

        // Historial de precios para notebook: 3 registros a lo largo del tiempo
        priceRepository.save(Price.builder()
                .product(notebook).store(mercado)
                .price(new BigDecimal("1200.00")).currency("USD")
                .recordedAt(BASE)
                .build());
        priceRepository.save(Price.builder()
                .product(notebook).store(mercado)
                .price(new BigDecimal("1100.00")).currency("USD")
                .recordedAt(BASE.plusDays(10))
                .build());
        priceRepository.save(Price.builder()
                .product(notebook).store(falabella)
                .price(new BigDecimal("1150.00")).currency("USD")
                .recordedAt(BASE.plusDays(5))
                .build());

        // Un precio para mouse
        priceRepository.save(Price.builder()
                .product(mouse).store(mercado)
                .price(new BigDecimal("50.00")).currency("USD")
                .recordedAt(BASE)
                .build());
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save: persiste precio con ID y recordedAt auto-asignados")
    void save_persistsPriceWithId() {
        Price p = priceRepository.save(Price.builder()
                .product(notebook)
                .store(mercado)
                .price(new BigDecimal("999.99"))
                .currency("USD")
                .build());

        assertThat(p.getId()).isNotNull();
        assertThat(p.getRecordedAt()).isNotNull();
    }

    // ─── findByProductIdOrderByRecordedAtDesc ────────────────────────────────

    @Test
    @DisplayName("findByProductIdOrderByRecordedAtDesc: retorna precios del producto, más reciente primero")
    void findByProductId_returnsOrderedByDateDesc() {
        List<Price> prices = priceRepository
                .findByProductIdOrderByRecordedAtDesc(notebook.getId());

        assertThat(prices).hasSize(3);
        // El más reciente es BASE+10 días
        assertThat(prices.get(0).getRecordedAt()).isEqualTo(BASE.plusDays(10));
        assertThat(prices.get(0).getPrice()).isEqualByComparingTo("1100.00");
    }

    @Test
    @DisplayName("findByProductIdOrderByRecordedAtDesc: retorna vacío si producto sin precios")
    void findByProductId_returnsEmptyForProductWithNoPrices() {
        Product otro = productRepository.save(Product.builder()
                .name("Sin precios").store(mercado).build());
        assertThat(priceRepository.findByProductIdOrderByRecordedAtDesc(otro.getId())).isEmpty();
    }

    // ─── findByProductIdAndStoreId ────────────────────────────────────────────

    @Test
    @DisplayName("findByProductIdAndStoreId: filtra por producto y tienda")
    void findByProductIdAndStoreId_returnsFilteredPrices() {
        List<Price> prices = priceRepository
                .findByProductIdAndStoreId(notebook.getId(), falabella.getId());

        assertThat(prices).hasSize(1);
        assertThat(prices.get(0).getPrice()).isEqualByComparingTo("1150.00");
    }

    @Test
    @DisplayName("findByProductIdAndStoreId: retorna vacío si no hay precios para esa combinación")
    void findByProductIdAndStoreId_returnsEmptyWhenNoMatch() {
        List<Price> prices = priceRepository
                .findByProductIdAndStoreId(mouse.getId(), falabella.getId());
        assertThat(prices).isEmpty();
    }

    // ─── findLatestPriceByProduct ─────────────────────────────────────────────

    @Test
    @DisplayName("findLatestPriceByProduct: retorna el precio más reciente")
    void findLatestPriceByProduct_returnsMostRecent() {
        Optional<Price> latest = priceRepository.findLatestPriceByProduct(notebook.getId());
        assertThat(latest).isPresent();
        assertThat(latest.get().getPrice()).isEqualByComparingTo("1100.00");
        assertThat(latest.get().getRecordedAt()).isEqualTo(BASE.plusDays(10));
    }

    @Test
    @DisplayName("findLatestPriceByProduct: retorna vacío si producto sin precios")
    void findLatestPriceByProduct_returnsEmptyWhenNoPrices() {
        Product otro = productRepository.save(Product.builder()
                .name("Sin precios").store(mercado).build());
        assertThat(priceRepository.findLatestPriceByProduct(otro.getId())).isEmpty();
    }

    // ─── findPriceHistory ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findPriceHistory: retorna precios dentro del rango de fechas")
    void findPriceHistory_returnsWithinDateRange() {
        // Rango: del día 0 al día 7 → solo los 2 primeros registros (día 0 y día 5)
        List<Price> history = priceRepository.findPriceHistory(
                notebook.getId(), BASE, BASE.plusDays(7));

        assertThat(history).hasSize(2);
        // Ordenados ASC por fecha
        assertThat(history.get(0).getRecordedAt()).isEqualTo(BASE);
        assertThat(history.get(1).getRecordedAt()).isEqualTo(BASE.plusDays(5));
    }

    @Test
    @DisplayName("findPriceHistory: retorna todos los precios en rango amplio")
    void findPriceHistory_returnsAllInBroadRange() {
        List<Price> history = priceRepository.findPriceHistory(
                notebook.getId(), BASE.minusDays(1), BASE.plusDays(30));
        assertThat(history).hasSize(3);
    }

    @Test
    @DisplayName("findPriceHistory: retorna vacío si rango no contiene precios")
    void findPriceHistory_returnsEmptyForOutOfRangeDate() {
        List<Price> history = priceRepository.findPriceHistory(
                notebook.getId(),
                BASE.plusDays(20),
                BASE.plusDays(30));
        assertThat(history).isEmpty();
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina un precio")
    void delete_removesPrice() {
        long countBefore = priceRepository.count();
        Price toDelete = priceRepository
                .findByProductIdOrderByRecordedAtDesc(mouse.getId()).get(0);
        priceRepository.delete(toDelete);
        assertThat(priceRepository.count()).isEqualTo(countBefore - 1);
    }
}
