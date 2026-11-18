package com.apiprecios.service;

import com.apiprecios.entity.Price;
import com.apiprecios.entity.Product;
import com.apiprecios.repository.PriceRepository;
import com.apiprecios.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cálculo verificable de una cesta de compras. Esto NO es un modelo de IA:
 * Java calcula los importes y el modelo (opcional) solamente los explica.
 * Los precios provienen del último registro por producto y tienda.
 */
@Service
public class ShoppingAdviceService {
    private final PriceRepository priceRepository;
    private final ProductRepository productRepository;

    public ShoppingAdviceService(PriceRepository priceRepository, ProductRepository productRepository) {
        this.priceRepository = priceRepository;
        this.productRepository = productRepository;
    }

    public record ShopItem(Integer productId, String productName, String storeName, Integer storeId,
                           BigDecimal price, String currency) { }
    public record BasketResult(List<ShopItem> cheapestItems, BigDecimal splitTotal,
                               String currency, List<String> missingProducts,
                               String oneStoreName, BigDecimal oneStoreTotal,
                               BigDecimal extraCostForOneStore, String notice) { }

    @Transactional(readOnly = true)
    public BasketResult compareBasket(List<Integer> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 10 || ids.stream().anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí entre 1 y 10 productos.");
        }
        List<Integer> uniqueIds = ids.stream().distinct().toList();
        List<ShopItem> cheapest = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Map<Integer, Map<Integer, ShopItem>> perProductStores = new LinkedHashMap<>();
        String currency = null;

        for (Integer id : uniqueIds) {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto inexistente: " + id));
            List<Price> available = priceRepository.findLatestByProductOrderByPriceAsc(id);
            Map<Integer, ShopItem> stores = new HashMap<>();
            for (Price p : available) {
                if (p.getStore() == null || p.getPrice() == null || p.getPrice().signum() < 0) continue;
                String itemCurrency = p.getCurrency() == null ? "ARS" : p.getCurrency();
                if (currency == null) currency = itemCurrency;
                if (!currency.equals(itemCurrency)) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "No se pueden sumar precios de diferentes monedas.");
                }
                ShopItem item = new ShopItem(id, product.getName(), p.getStore().getName(),
                        p.getStore().getId(), p.getPrice(), itemCurrency);
                stores.merge(item.storeId(), item,
                        (left, right) -> left.price().compareTo(right.price()) <= 0 ? left : right);
            }
            if (stores.isEmpty()) {
                missing.add(product.getName());
            } else {
                ShopItem best = stores.values().stream()
                        .min(Comparator.comparing(ShopItem::price)).orElseThrow();
                cheapest.add(best);
                perProductStores.put(id, stores);
            }
        }
        BigDecimal splitTotal = cheapest.stream().map(ShopItem::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Integer, BigDecimal> singleStoreTotals = new HashMap<>();
        Map<Integer, String> storeNames = new HashMap<>();
        if (missing.isEmpty()) {
            for (Map<Integer, ShopItem> stores : perProductStores.values()) {
                for (ShopItem item : stores.values()) {
                    storeNames.put(item.storeId(), item.storeName());
                    singleStoreTotals.merge(item.storeId(), item.price(), BigDecimal::add);
                }
            }
        }
        Integer bestOneStoreId = singleStoreTotals.entrySet().stream()
                .filter(e -> perProductStores.values().stream().allMatch(m -> m.containsKey(e.getKey())))
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
        BigDecimal oneTotal = bestOneStoreId == null ? null : singleStoreTotals.get(bestOneStoreId);
        return new BasketResult(cheapest, splitTotal, currency == null ? "ARS" : currency,
                missing, bestOneStoreId == null ? null : storeNames.get(bestOneStoreId), oneTotal,
                oneTotal == null ? null : oneTotal.subtract(splitTotal),
                "Comparación basada en precios registrados; no incluye transporte, stock ni calidad verificada. " +
                "Revisá la fecha de los precios antes de comprar.");
    }
}
