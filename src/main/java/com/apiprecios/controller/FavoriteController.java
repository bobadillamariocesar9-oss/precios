package com.apiprecios.controller;

import com.apiprecios.entity.Favorite;
import com.apiprecios.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Gestión de favoritos")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    @Operation(summary = "Listar todos los favoritos")
    public List<Favorite> findAll() {
        return favoriteService.findAll();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Listar favoritos de un usuario")
    public List<Favorite> findByUser(@PathVariable Integer userId) {
        return favoriteService.findByUser(userId);
    }

    @GetMapping("/check")
    @Operation(summary = "Verificar si un producto es favorito del usuario")
    public boolean isFavorite(@RequestParam Integer userId, @RequestParam Integer productId) {
        return favoriteService.isFavorite(userId, productId);
    }

    @PostMapping
    @Operation(summary = "Agregar producto a favoritos")
    public ResponseEntity<Favorite> add(@RequestBody FavoriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(favoriteService.add(request.userId(), request.productId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar favorito por ID")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        favoriteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Quitar producto de favoritos por usuario y producto")
    public ResponseEntity<Void> remove(@RequestParam Integer userId, @RequestParam Integer productId) {
        favoriteService.remove(userId, productId);
        return ResponseEntity.noContent().build();
    }

    public record FavoriteRequest(@NotNull Integer userId, @NotNull Integer productId) {}
}
