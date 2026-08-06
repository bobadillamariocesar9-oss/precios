package com.apiprecios.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyección de solo lectura: para cada producto, su precio más barato
 * vigente entre todas las tiendas, con la ubicación del proveedor.
 */
public interface CheapestPriceProjection {
    Integer getProductId();
    String getProductName();
    String getBrand();
    String getCategory();
    BigDecimal getPrice();
    String getCurrency();
    LocalDateTime getRecordedAt();
    Integer getStoreId();
    String getStoreName();
    String getStoreAddress();
}
