package com.apiprecios.service;

import com.apiprecios.entity.Price;
import com.apiprecios.entity.Product;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.PriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceService")
class PriceServiceTest {

    @Mock
    private PriceRepository priceRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private PriceService priceService;

    private Product notebook;
    private Price price1;
    private Price price2;
    private final LocalDateTime BASE = LocalDateTime.of(2025, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        notebook = Product.builder().name("Notebook").build();
        notebook.setId(1);

        price1 = Price.builder()
                .product(notebook)
                .price(new BigDecimal("1200.00"))
                .currency("USD")
                .recordedAt(BASE)
                .build();
        price2 = Price.builder()
                .product(notebook)
                .price(new BigDecimal("1100.00"))
                .currency("USD")
                .recordedAt(BASE.plusDays(10))
                .build();
    }

    // ─── findByProduct ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByProduct: valida producto y retorna precios ordenados")
    void findByProduct_validatesAndReturnsPrices() {
        when(productService.findById(1)).thenReturn(notebook);
        when(priceRepository.findByProductIdOrderByRecordedAtDesc(1))
                .thenReturn(List.of(price2, price1));

        List<Price> result = priceService.findByProduct(1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPrice()).isEqualByComparingTo("1100.00");
        verify(productService).findById(1);
    }

    @Test
    @DisplayName("findByProduct: lanza excepción si el producto no existe")
    void findByProduct_throwsWhenProductNotFound() {
        when(productService.findById(99))
                .thenThrow(new ResourceNotFoundException("Product", 99));

        assertThatThrownBy(() -> priceService.findByProduct(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceRepository, never()).findByProductIdOrderByRecordedAtDesc(any());
    }

    // ─── findLatest ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findLatest: retorna el precio más reciente")
    void findLatest_returnsMostRecentPrice() {
        when(productService.findById(1)).thenReturn(notebook);
        when(priceRepository.findLatestPriceByProduct(1)).thenReturn(Optional.of(price2));

        Price result = priceService.findLatest(1);

        assertThat(result.getPrice()).isEqualByComparingTo("1100.00");
    }

    @Test
    @DisplayName("findLatest: lanza ResourceNotFoundException si no hay precios")
    void findLatest_throwsWhenNoPrices() {
        when(productService.findById(1)).thenReturn(notebook);
        when(priceRepository.findLatestPriceByProduct(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.findLatest(1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    @DisplayName("findLatest: lanza excepción si el producto no existe")
    void findLatest_throwsWhenProductNotFound() {
        when(productService.findById(99))
                .thenThrow(new ResourceNotFoundException("Product", 99));

        assertThatThrownBy(() -> priceService.findLatest(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── findHistory ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findHistory: valida producto y retorna rango de fechas")
    void findHistory_validatesAndReturnsRange() {
        LocalDateTime from = BASE;
        LocalDateTime to = BASE.plusDays(30);

        when(productService.findById(1)).thenReturn(notebook);
        when(priceRepository.findPriceHistory(1, from, to))
                .thenReturn(List.of(price1, price2));

        List<Price> result = priceService.findHistory(1, from, to);

        assertThat(result).hasSize(2);
        verify(priceRepository).findPriceHistory(1, from, to);
    }

    @Test
    @DisplayName("findHistory: retorna lista vacía si no hay precios en el rango")
    void findHistory_returnsEmptyForOutOfRangeDate() {
        LocalDateTime from = BASE.plusDays(100);
        LocalDateTime to = BASE.plusDays(200);

        when(productService.findById(1)).thenReturn(notebook);
        when(priceRepository.findPriceHistory(1, from, to)).thenReturn(List.of());

        assertThat(priceService.findHistory(1, from, to)).isEmpty();
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: valida producto y guarda el precio")
    void register_validatesProductAndSaves() {
        Price newPrice = Price.builder()
                .product(notebook)
                .price(new BigDecimal("950.00"))
                .currency("USD")
                .build();
        notebook.setId(1);

        when(productService.findById(1)).thenReturn(notebook);
        when(priceRepository.save(newPrice)).thenReturn(newPrice);

        Price result = priceService.register(newPrice);

        assertThat(result.getPrice()).isEqualByComparingTo("950.00");
        verify(priceRepository).save(newPrice);
    }

    @Test
    @DisplayName("register: lanza excepción si el producto no existe")
    void register_throwsWhenProductNotFound() {
        Price newPrice = Price.builder()
                .product(notebook)
                .price(new BigDecimal("950.00"))
                .build();
        notebook.setId(99);

        when(productService.findById(99))
                .thenThrow(new ResourceNotFoundException("Product", 99));

        assertThatThrownBy(() -> priceService.register(newPrice))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceRepository, never()).save(any());
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina precio existente")
    void delete_removesPrice() {
        when(priceRepository.findById(1)).thenReturn(Optional.of(price1));

        priceService.delete(1);

        verify(priceRepository).delete(price1);
    }

    @Test
    @DisplayName("delete: lanza ResourceNotFoundException si precio no existe")
    void delete_throwsWhenNotFound() {
        when(priceRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.delete(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceRepository, never()).delete(any());
    }
}
