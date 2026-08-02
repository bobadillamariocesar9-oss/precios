package com.apiprecios.repository;

import com.apiprecios.entity.Price;
import com.apiprecios.repository.projection.CheapestPriceProjection;
import com.apiprecios.repository.projection.NearbyPriceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceRepository extends JpaRepository<Price, Integer> {

    List<Price> findByProductIdOrderByRecordedAtDesc(Integer productId);

    List<Price> findByProductIdAndStoreId(Integer productId, Integer storeId);

    @Query("SELECT p FROM Price p WHERE p.product.id = :productId ORDER BY p.recordedAt DESC LIMIT 1")
    Optional<Price> findLatestPriceByProduct(@Param("productId") Integer productId);

    @Query("SELECT p FROM Price p WHERE p.product.id = :productId AND p.recordedAt BETWEEN :from AND :to ORDER BY p.recordedAt ASC")
    List<Price> findPriceHistory(@Param("productId") Integer productId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    /**
     * Precios de un producto ordenados de menor a mayor, con la tienda
     * (JOIN FETCH) ya cargada para poder mostrar su ubicación sin
     * necesidad de una sesión Hibernate abierta (open-in-view: false).
     * Si hay precios repetidos del mismo producto/tienda, solo trae el
     * más reciente de cada tienda.
     */
    @Query("""
        SELECT p FROM Price p
        JOIN FETCH p.store s
        WHERE p.product.id = :productId
          AND p.recordedAt = (
              SELECT MAX(p2.recordedAt) FROM Price p2
              WHERE p2.product.id = :productId AND p2.store.id = s.id
          )
        ORDER BY p.price ASC
        """)
    List<Price> findLatestByProductOrderByPriceAsc(@Param("productId") Integer productId);

    /**
     * Precios de un producto en tiendas dentro de un radio (km) desde
     * (lat, lng), ordenados de menor a mayor precio, con la distancia
     * calculada vía la fórmula de Haversine (SQL nativo — PostgreSQL
     * no tiene trigonometría en JPQL).
     *
     * LEAST/GREATEST recortan el argumento de acos() a [-1, 1] para
     * evitar errores de dominio por redondeo de punto flotante.
     */
    @Query(value = """
        SELECT
            pr.id                                                  AS id,
            pr.price                                                AS price,
            pr.currency                                             AS currency,
            pr.recorded_at                                          AS recordedAt,
            s.id                                                    AS storeId,
            s.name                                                  AS storeName,
            s.address                                               AS storeAddress,
            (6371 * acos(LEAST(1, GREATEST(-1,
                cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(s.latitude))
            )))) AS distanceKm
        FROM prices pr
        JOIN stores s ON s.id = pr.store_id
        WHERE pr.product_id = :productId
          AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL
          AND pr.recorded_at = (
              SELECT MAX(p2.recorded_at) FROM prices p2
              WHERE p2.product_id = :productId AND p2.store_id = s.id
          )
          AND (6371 * acos(LEAST(1, GREATEST(-1,
                cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(s.latitude))
              )))) <= :radiusKm
        ORDER BY pr.price ASC
        """, nativeQuery = true)
    List<NearbyPriceProjection> findNearbyByProductOrderByPriceAsc(
            @Param("productId") Integer productId,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm);

    /**
     * Para cada producto que tenga al menos un precio cargado, el precio
     * más barato vigente (el más reciente de cada tienda, y entre esos
     * el mínimo), con la ubicación del proveedor. Toda la lista ordenada
     * de menor a mayor precio, sin importar el producto.
     *
     * Dos pasos con window functions (SQL nativo):
     *   1. current_prices: el precio más reciente de cada par producto+tienda
     *   2. cheapest: entre esos, el más barato por producto (price_rank = 1)
     */
    @Query(value = """
        WITH current_prices AS (
            SELECT pr.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY pr.product_id, pr.store_id
                       ORDER BY pr.recorded_at DESC
                   ) AS rn
            FROM prices pr
        ),
        cheapest AS (
            SELECT cp.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY cp.product_id
                       ORDER BY cp.price ASC
                   ) AS price_rank
            FROM current_prices cp
            WHERE cp.rn = 1
        )
        SELECT
            p.id            AS productId,
            p.name          AS productName,
            p.brand         AS brand,
            p.category      AS category,
            c.price         AS price,
            c.currency      AS currency,
            c.recorded_at   AS recordedAt,
            s.id            AS storeId,
            s.name          AS storeName,
            s.address       AS storeAddress
        FROM cheapest c
        JOIN products p ON p.id = c.product_id
        JOIN stores   s ON s.id = c.store_id
        WHERE c.price_rank = 1
        ORDER BY c.price ASC
        """, nativeQuery = true)
    List<CheapestPriceProjection> findCheapestPricePerProduct();
}
