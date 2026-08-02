package com.apiprecios.controller;

import com.apiprecios.entity.PriceAlert;
import com.apiprecios.entity.Product;
import com.apiprecios.entity.User;
import com.apiprecios.service.PriceAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/price-alerts")
@RequiredArgsConstructor
@Tag(name = "PriceAlerts", description = "Gestión de alertas de precio")
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    @GetMapping
    @Operation(summary = "Listar todas las alertas")
    public List<PriceAlert> findAll() {
        return priceAlertService.findAll();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Listar alertas de un usuario")
    public List<PriceAlert> findByUser(@PathVariable Integer userId) {
        return priceAlertService.findByUser(userId);
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Listar alertas activas de un usuario")
    public List<PriceAlert> findActiveByUser(@PathVariable Integer userId) {
        return priceAlertService.findActiveByUser(userId);
    }

    @GetMapping("/product/{productId}/active")
    @Operation(summary = "Listar alertas activas de un producto")
    public List<PriceAlert> findActiveByProduct(@PathVariable Integer productId) {
        return priceAlertService.findActiveByProduct(productId);
    }

    @PostMapping
    @Operation(summary = "Crear nueva alerta de precio")
    public ResponseEntity<PriceAlert> create(@RequestBody PriceAlertRequest request) {
        PriceAlert alert = PriceAlert.builder()
                .user(User.builder().id(request.userId()).build())
                .product(Product.builder().id(request.productId()).build())
                .targetPrice(request.targetPrice())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(priceAlertService.create(alert));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar alerta")
    public PriceAlert deactivate(@PathVariable Integer id) {
        return priceAlertService.deactivate(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar alerta")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        priceAlertService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record PriceAlertRequest(
            @NotNull Integer userId,
            @NotNull Integer productId,
            @NotNull BigDecimal targetPrice) {}
}
