package com.apiprecios.controller;

import com.apiprecios.service.ShoppingAiExplanationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-advice")
public class ShoppingAiExplanationController {
    private final ShoppingAiExplanationService ai;
    public ShoppingAiExplanationController(ShoppingAiExplanationService ai) { this.ai = ai; }

    public record ExplainRequest(@NotEmpty @Size(max = 10) List<Integer> productIds) { }

    // POST requiere autenticación según SecurityConfig; evita IA anónima.
    @PostMapping("/explain")
    public ShoppingAiExplanationService.ExplainedBasket explain(
            @Valid @RequestBody ExplainRequest request) {
        return ai.explain(request.productIds());
    }
}
