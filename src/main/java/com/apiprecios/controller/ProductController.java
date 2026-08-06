package com.apiprecios.controller;

import com.apiprecios.entity.Product;
import com.apiprecios.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Gestión de productos")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Listar todos los productos")
    public List<Product> findAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID")
    public Product findById(@PathVariable Integer id) {
        return productService.findById(id);
    }

    @GetMapping("/store/{storeId}")
    @Operation(summary = "Listar productos de una tienda")
    public List<Product> findByStore(@PathVariable Integer storeId) {
        return productService.findByStore(storeId);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar productos por palabra clave")
    public List<Product> search(@RequestParam(required = false) String keyword) {
        return productService.search(keyword);
    }

    @PostMapping
    @Operation(summary = "Crear nuevo producto")
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    public Product update(@PathVariable Integer id, @Valid @RequestBody Product product) {
        return productService.update(id, product);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
