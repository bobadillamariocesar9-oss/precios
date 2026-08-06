package com.apiprecios.repository;

import com.apiprecios.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByStoreId(Integer storeId);

    List<Product> findByNameContainingIgnoreCase(String name);

    @Query("SELECT p FROM Product p JOIN FETCH p.store WHERE p.id = :id")
    Product findByIdWithStore(@Param("id") Integer id);

    @Query("SELECT p FROM Product p WHERE p.name ILIKE %:keyword% OR p.description ILIKE %:keyword%")
    List<Product> searchByKeyword(@Param("keyword") String keyword);
}
