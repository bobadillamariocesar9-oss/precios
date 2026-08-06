package com.apiprecios.controller;

import com.apiprecios.entity.Store;
import com.apiprecios.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "Gestión de tiendas")
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    @Operation(summary = "Listar todas las tiendas")
    public List<Store> findAll() {
        return storeService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener tienda por ID")
    public Store findById(@PathVariable Integer id) {
        return storeService.findById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar tiendas por nombre")
    public List<Store> search(@RequestParam String name) {
        return storeService.search(name);
    }

    @PostMapping
    @Operation(summary = "Crear nueva tienda")
    public ResponseEntity<Store> create(@Valid @RequestBody Store store) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.create(store));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar tienda")
    public Store update(@PathVariable Integer id, @Valid @RequestBody Store store) {
        return storeService.update(id, store);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar tienda")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        storeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
