package com.apiprecios.controller;

import com.apiprecios.entity.Price;
import com.apiprecios.repository.projection.CheapestPriceProjection;
import com.apiprecios.repository.projection.NearbyPriceProjection;
import com.apiprecios.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
@Tag(name = "Prices", description = "Gestión de precios")
public class PriceController {

    private final PriceService priceService;

    @GetMapping("/cheapest")
    @Operation(summary = "Lista de todos los productos con precio cargado, "
            + "cada uno con su precio más barato vigente y la ubicación del proveedor, "
            + "ordenada de menor a mayor")
    public List<CheapestPriceProjection> findAllCheapestPrices() {
        return priceService.findAllCheapestPrices();
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Historial de precios de un producto")
    public List<Price> findByProduct(@PathVariable Integer productId) {
        return priceService.findByProduct(productId);
    }

    @GetMapping("/product/{productId}/latest")
    @Operation(summary = "Último precio registrado de un producto")
    public Price findLatest(@PathVariable Integer productId) {
        return priceService.findLatest(productId);
    }

    @GetMapping("/product/{productId}/compare")
    @Operation(summary = "Compara precios de un producto entre tiendas, "
            + "ordenados de menor a mayor, incluyendo la ubicación del proveedor")
    public List<Price> comparePrices(@PathVariable Integer productId) {
        return priceService.comparePrices(productId);
    }

    @GetMapping("/product/{productId}/compare/nearby")
    @Operation(summary = "Compara precios de un producto solo entre tiendas dentro de un "
            + "radio (km) desde la ubicación del usuario, ordenados de menor a mayor, "
            + "con la distancia a cada una")
    public List<NearbyPriceProjection> comparePricesNearby(
            @PathVariable Integer productId,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm) {
        return priceService.comparePricesNearby(productId, lat, lng, radiusKm);
    }

    @GetMapping("/product/{productId}/history")
    @Operation(summary = "Historial de precios en un rango de fechas")
    public List<Price> findHistory(
            @PathVariable Integer productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return priceService.findHistory(productId, from, to);
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo precio")
    public ResponseEntity<Price> register(@Valid @RequestBody Price price) {
        return ResponseEntity.status(HttpStatus.CREATED).body(priceService.register(price));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar registro de precio")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        priceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
