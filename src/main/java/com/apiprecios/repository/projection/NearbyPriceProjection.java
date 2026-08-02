package com.apiprecios.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyección de solo lectura: precio de un producto en una tienda dentro
 * de un radio, con la distancia en kilómetros ya calculada por la base
 * de datos (fórmula de Haversine).
 */
public interface NearbyPriceProjection {
    Integer getId();
    BigDecimal getPrice();
    String getCurrency();
    LocalDateTime getRecordedAt();
    Integer getStoreId();
    String getStoreName();
    String getStoreAddress();
    Double getDistanceKm();
}
