package com.apiprecios.repository;

import com.apiprecios.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer> {

    Optional<Store> findByName(String name);

    List<Store> findByNameContainingIgnoreCase(String name);
}
