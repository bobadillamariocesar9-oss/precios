package com.apiprecios.service;

import com.apiprecios.entity.Product;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final StoreService storeService;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public Product findByIdWithStore(Integer id) {
        Product product = productRepository.findByIdWithStore(id);
        if (product == null) {
            throw new ResourceNotFoundException("Product", id);
        }
        return product;
    }

    public List<Product> findByStore(Integer storeId) {
        // Valida que la tienda exista
        storeService.findById(storeId);
        return productRepository.findByStoreId(storeId);
    }

    public List<Product> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        return productRepository.searchByKeyword(keyword.trim());
    }

    @Transactional
    public Product create(Product product) {
        // Valida que la tienda exista
        if (product.getStore() != null && product.getStore().getId() != null) {
            storeService.findById(product.getStore().getId());
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Integer id, Product data) {
        Product product = findById(id);
        product.setName(data.getName());
        product.setBrand(data.getBrand());
        product.setDescription(data.getDescription());
        product.setCategory(data.getCategory());
        product.setImageUrl(data.getImageUrl());
        product.setUrl(data.getUrl());
        if (data.getStore() != null && data.getStore().getId() != null) {
            product.setStore(storeService.findById(data.getStore().getId()));
        }
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Integer id) {
        Product product = findById(id);
        productRepository.delete(product);
    }
}
