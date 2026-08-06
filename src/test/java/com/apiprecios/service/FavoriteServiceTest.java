package com.apiprecios.service;

import com.apiprecios.entity.Favorite;
import com.apiprecios.entity.Product;
import com.apiprecios.entity.User;
import com.apiprecios.exception.BadRequestException;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.FavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService")
class FavoriteServiceTest {

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private UserService userService;
    @Mock private ProductService productService;

    @InjectMocks
    private FavoriteService favoriteService;

    private User mario;
    private Product notebook;
    private Favorite favorite;

    @BeforeEach
    void setUp() {
        mario = User.builder().username("mario").email("m@test.com").password("p").build();
        mario.setId(1);

        notebook = Product.builder().name("Notebook").build();
        notebook.setId(1);

        favorite = Favorite.builder().user(mario).product(notebook).build();
    }

    // ─── findByUser ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUser: valida usuario y retorna sus favoritos")
    void findByUser_validatesUserAndReturnsFavorites() {
        when(userService.findById(1)).thenReturn(mario);
        when(favoriteRepository.findByUserId(1)).thenReturn(List.of(favorite));

        List<Favorite> result = favoriteService.findByUser(1);

        assertThat(result).containsExactly(favorite);
        verify(userService).findById(1);
    }

    @Test
    @DisplayName("findByUser: lanza excepción si el usuario no existe")
    void findByUser_throwsWhenUserNotFound() {
        when(userService.findById(99))
                .thenThrow(new ResourceNotFoundException("User", 99));

        assertThatThrownBy(() -> favoriteService.findByUser(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(favoriteRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("findByUser: retorna lista vacía si usuario sin favoritos")
    void findByUser_returnsEmptyWhenNoFavorites() {
        when(userService.findById(1)).thenReturn(mario);
        when(favoriteRepository.findByUserId(1)).thenReturn(List.of());

        assertThat(favoriteService.findByUser(1)).isEmpty();
    }

    // ─── isFavorite ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("isFavorite: retorna true cuando el favorito existe")
    void isFavorite_returnsTrueWhenExists() {
        when(favoriteRepository.existsByUserIdAndProductId(1, 1)).thenReturn(true);

        assertThat(favoriteService.isFavorite(1, 1)).isTrue();
    }

    @Test
    @DisplayName("isFavorite: retorna false cuando el favorito no existe")
    void isFavorite_returnsFalseWhenNotExists() {
        when(favoriteRepository.existsByUserIdAndProductId(1, 99)).thenReturn(false);

        assertThat(favoriteService.isFavorite(1, 99)).isFalse();
    }

    // ─── add ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("add: valida user y product y crea el favorito")
    void add_createsNewFavorite() {
        when(favoriteRepository.existsByUserIdAndProductId(1, 1)).thenReturn(false);
        when(userService.findById(1)).thenReturn(mario);
        when(productService.findById(1)).thenReturn(notebook);
        when(favoriteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Favorite result = favoriteService.add(1, 1);

        assertThat(result.getUser()).isEqualTo(mario);
        assertThat(result.getProduct()).isEqualTo(notebook);
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("add: lanza BadRequestException si ya es favorito")
    void add_throwsWhenAlreadyFavorite() {
        when(favoriteRepository.existsByUserIdAndProductId(1, 1)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.add(1, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("favoritos");

        verify(favoriteRepository, never()).save(any());
        verify(userService, never()).findById(any());
    }

    @Test
    @DisplayName("add: lanza excepción si el usuario no existe")
    void add_throwsWhenUserNotFound() {
        when(favoriteRepository.existsByUserIdAndProductId(99, 1)).thenReturn(false);
        when(userService.findById(99))
                .thenThrow(new ResourceNotFoundException("User", 99));

        assertThatThrownBy(() -> favoriteService.add(99, 1))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("add: lanza excepción si el producto no existe")
    void add_throwsWhenProductNotFound() {
        when(favoriteRepository.existsByUserIdAndProductId(1, 99)).thenReturn(false);
        when(userService.findById(1)).thenReturn(mario);
        when(productService.findById(99))
                .thenThrow(new ResourceNotFoundException("Product", 99));

        assertThatThrownBy(() -> favoriteService.add(1, 99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(favoriteRepository, never()).save(any());
    }

    // ─── remove ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("remove: elimina favorito existente")
    void remove_deletesExistingFavorite() {
        when(favoriteRepository.existsByUserIdAndProductId(1, 1)).thenReturn(true);

        favoriteService.remove(1, 1);

        verify(favoriteRepository).deleteByUserIdAndProductId(1, 1);
    }

    @Test
    @DisplayName("remove: lanza BadRequestException si no es favorito")
    void remove_throwsWhenNotFavorite() {
        when(favoriteRepository.existsByUserIdAndProductId(1, 99)).thenReturn(false);

        assertThatThrownBy(() -> favoriteService.remove(1, 99))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("favoritos");

        verify(favoriteRepository, never()).deleteByUserIdAndProductId(any(), any());
    }
}
