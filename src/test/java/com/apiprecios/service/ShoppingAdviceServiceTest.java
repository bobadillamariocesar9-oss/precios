package com.apiprecios.service;

import com.apiprecios.entity.Price;
import com.apiprecios.entity.Product;
import com.apiprecios.entity.Store;
import com.apiprecios.repository.PriceRepository;
import com.apiprecios.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingAdviceServiceTest {
    @Mock PriceRepository prices;
    @Mock ProductRepository products;
    @InjectMocks ShoppingAdviceService advice;

    private Price price(int storeId, String storeName, String value) {
        return Price.builder().price(new BigDecimal(value)).currency("ARS")
                .store(Store.builder().id(storeId).name(storeName).build()).build();
    }

    @Test
    void comparesSplitBasketToOneStoreUsingPricesFromDatabase() {
        when(products.findById(1)).thenReturn(Optional.of(Product.builder().id(1).name("Arroz").build()));
        when(products.findById(2)).thenReturn(Optional.of(Product.builder().id(2).name("Leche").build()));
        when(prices.findLatestByProductOrderByPriceAsc(1))
                .thenReturn(List.of(price(1, "Tienda A", "100"), price(2, "Tienda B", "120")));
        when(prices.findLatestByProductOrderByPriceAsc(2))
                .thenReturn(List.of(price(1, "Tienda A", "150"), price(2, "Tienda B", "130")));
        var result = advice.compareBasket(List.of(1, 2, 1));
        assertEquals(new BigDecimal("230"), result.splitTotal());
        assertEquals("Tienda A", result.oneStoreName());
        assertEquals(new BigDecimal("250"), result.oneStoreTotal());
        assertEquals(new BigDecimal("20"), result.extraCostForOneStore());
        assertEquals(2, result.cheapestItems().size());
    }

    @Test
    void notesWhenAProductHasNoRecordedPrice() {
        when(products.findById(1)).thenReturn(Optional.of(Product.builder().id(1).name("Arroz").build()));
        when(prices.findLatestByProductOrderByPriceAsc(1)).thenReturn(List.of());
        var result = advice.compareBasket(List.of(1));
        assertEquals(List.of("Arroz"), result.missingProducts());
        assertNull(result.oneStoreTotal());
    }
}
