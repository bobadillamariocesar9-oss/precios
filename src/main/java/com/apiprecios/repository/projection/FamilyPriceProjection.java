package com.apiprecios.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyección de solo lectura: precio vigente (el más reciente por
 * producto+tienda) de TODOS los productos cuyo nombre pertenece a una
 * misma "familia" (ej. todos los "Arroz ..."), sin filtrar al más
 * barato por producto — a diferencia de {@link CheapestPriceProjection},
 * acá puede haber varias filas para el mismo producto (una por tienda).
 */
public interface FamilyPriceProjection {
    Integer getId();
    BigDecimal getPrice();
    String getCurrency();
    LocalDateTime getRecordedAt();
    Integer getProductId();
    String getProductName();
    String getBrand();
    String getCategory();
    Integer getStoreId();
    String getStoreName();
    String getStoreAddress();
}
