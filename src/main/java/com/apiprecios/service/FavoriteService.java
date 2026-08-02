package com.apiprecios.service;

import com.apiprecios.entity.Favorite;
import com.apiprecios.entity.Product;
import com.apiprecios.entity.User;
import com.apiprecios.exception.BadRequestException;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserService userService ;
    private final ProductService productService ;

    public List<Favorite> findAll() {
        return favoriteRepository.findAll();
    }

    public List<Favorite> findByUser(Integer userId) {
        userService.findById(userId);
        return favoriteRepository.findByUserId(userId);
    }

    public boolean isFavorite(Integer userId, Integer productId) {
        return favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public Favorite add(Integer userId, Integer productId) {
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BadRequestException("El producto ya está en favoritos del usuario");
        }
        User user = userService.findById(userId);
        Product product = productService.findById(productId);

        return favoriteRepository.save(Favorite.builder()
                .user(user)
                .product(product)
                .build());
    }

    @Transactional
    public void remove(Integer userId, Integer productId) {
        if (!favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BadRequestException("El producto no está en favoritos del usuario");
        }
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void deleteById(Integer id) {
        if (!favoriteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Favorite", id);
        }
        favoriteRepository.deleteById(id);
    }
}
