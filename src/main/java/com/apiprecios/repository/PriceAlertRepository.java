package com.apiprecios.repository;

import com.apiprecios.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Integer> {

    List<PriceAlert> findByUserId(Integer userId);

    List<PriceAlert> findByUserIdAndActiveTrue(Integer userId);

    List<PriceAlert> findByProductIdAndActiveTrue(Integer productId);

    @Query("SELECT pa FROM PriceAlert pa WHERE pa.active = true AND pa.product.id = :productId")
    List<PriceAlert> findActiveAlertsByProduct(@Param("productId") Integer productId);
}
