package com.apiprecios.controller;

import com.apiprecios.service.ShoppingAdviceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-advice")
public class ShoppingAdviceController {
    private final ShoppingAdviceService advice;
    public ShoppingAdviceController(ShoppingAdviceService advice) { this.advice = advice; }

    // Solo cálculos verificables; no necesita una API externa.
    public record BasketRequest(@NotEmpty @Size(max = 10) List<Integer> productIds) { }

    @PostMapping("/compare")
    public ShoppingAdviceService.BasketResult compare(@Valid @RequestBody BasketRequest request) {
        return advice.compareBasket(request.productIds());
    }
}
