package com.apiprecios.service;

import com.apiprecios.entity.Price;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.PriceRepository;
import com.apiprecios.repository.projection.CheapestPriceProjection;
import com.apiprecios.repository.projection.NearbyPriceProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceService {

    private final PriceRepository priceRepository;
    private final ProductService productService;

    public List<Price> findByProduct(Integer productId) {
        productService.findById(productId);
        return priceRepository.findByProductIdOrderByRecordedAtDesc(productId);
    }

    /**
     * Compara precios de un producto entre tiendas: el precio más reciente
     * de cada tienda, ordenado de menor a mayor, con la ubicación del
     * proveedor incluida.
     */
    public List<Price> comparePrices(Integer productId) {
        productService.findById(productId);
        return priceRepository.findLatestByProductOrderByPriceAsc(productId);
    }

    /**
     * Igual que comparePrices, pero solo entre tiendas dentro de un radio
     * (en km) desde la ubicación del usuario, con la distancia incluida.
     */
    public List<NearbyPriceProjection> comparePricesNearby(
            Integer productId, double lat, double lng, double radiusKm) {
        productService.findById(productId);
        return priceRepository.findNearbyByProductOrderByPriceAsc(productId, lat, lng, radiusKm);
    }

    /**
     * Lista de todos los productos con precio cargado, cada uno con su
     * precio más barato vigente, ordenada de menor a mayor.
     */
    public List<CheapestPriceProjection> findAllCheapestPrices() {
        return priceRepository.findCheapestPricePerProduct();
    }

    public Price findLatest(Integer productId) {
        productService.findById(productId);
        return priceRepository.findLatestPriceByProduct(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay precios registrados para el producto con id " + productId));
    }

    public List<Price> findHistory(Integer productId, LocalDateTime from, LocalDateTime to) {
        productService.findById(productId);
        return priceRepository.findPriceHistory(productId, from, to);
    }

    @Transactional
    public Price register(Price price) {
        // Valida que el producto exista
        if (price.getProduct() != null && price.getProduct().getId() != null) {
            productService.findById(price.getProduct().getId());
        }
        return priceRepository.save(price);
    }

    @Transactional
    public void delete(Integer id) {
        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Price", id));
        priceRepository.delete(price);
    }
}
