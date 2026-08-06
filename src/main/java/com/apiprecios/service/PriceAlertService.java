package com.apiprecios.service;

import com.apiprecios.entity.PriceAlert;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final UserService userService;
    private final ProductService productService;

    public List<PriceAlert> findAll() {
        return priceAlertRepository.findAll();
    }

    public List<PriceAlert> findByUser(Integer userId) {
        userService.findById(userId);
        return priceAlertRepository.findByUserId(userId);
    }

    public List<PriceAlert> findActiveByUser(Integer userId) {
        userService.findById(userId);
        return priceAlertRepository.findByUserIdAndActiveTrue(userId);
    }

    public List<PriceAlert> findActiveByProduct(Integer productId) {
        productService.findById(productId);
        return priceAlertRepository.findActiveAlertsByProduct(productId);
    }

    @Transactional
    public PriceAlert create(PriceAlert alert) {
        if (alert.getUser() != null && alert.getUser().getId() != null) {
            userService.findById(alert.getUser().getId());
        }
        if (alert.getProduct() != null && alert.getProduct().getId() != null) {
            productService.findById(alert.getProduct().getId());
        }
        alert.setActive(true);
        return priceAlertRepository.save(alert);
    }

    @Transactional
    public PriceAlert deactivate(Integer id) {
        PriceAlert alert = priceAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PriceAlert", id));
        alert.setActive(false);
        return priceAlertRepository.save(alert);
    }

    @Transactional
    public void delete(Integer id) {
        PriceAlert alert = priceAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PriceAlert", id));
        priceAlertRepository.delete(alert);
    }
}
