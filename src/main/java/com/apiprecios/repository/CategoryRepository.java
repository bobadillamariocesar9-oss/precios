package com.apiprecios.repository;

import com.apiprecios.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByNombreCategIgnoreCase(String nombreCateg);

    Optional<Category> findByCategIgnoreCase(String categ);

    List<Category> findAllByOrderByNombreCategAsc();

    boolean existsByNombreCategIgnoreCase(String nombreCateg);
}
