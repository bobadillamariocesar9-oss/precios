package com.apiprecios.service;

import com.apiprecios.entity.PriceAlert;
import com.apiprecios.entity.Product;
import com.apiprecios.entity.User;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceAlertService")
class PriceAlertServiceTest {

    @Mock private PriceAlertRepository priceAlertRepository;
    @Mock private UserService userService;
    @Mock private ProductService productService;

    @InjectMocks
    private PriceAlertService priceAlertService;

    private User mario;
    private Product notebook;
    private PriceAlert activeAlert;
    private PriceAlert inactiveAlert;

    @BeforeEach
    void setUp() {
        mario = User.builder().username("mario").email("mario@test.com").password("p").build();
        mario.setId(1);

        notebook = Product.builder().name("Notebook").build();
        notebook.setId(1);

        activeAlert = PriceAlert.builder()
                .user(mario).product(notebook)
                .targetPrice(new BigDecimal("900.00")).active(true).build();

        inactiveAlert = PriceAlert.builder()
                .user(mario).product(notebook)
                .targetPrice(new BigDecimal("800.00")).active(false).build();
    }

    // ─── findByUser ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUser: valida usuario y retorna todas sus alertas")
    void findByUser_validatesUserAndReturnsAll() {
        when(userService.findById(1)).thenReturn(mario);
        when(priceAlertRepository.findByUserId(1))
                .thenReturn(List.of(activeAlert, inactiveAlert));

        List<PriceAlert> result = priceAlertService.findByUser(1);

        assertThat(result).hasSize(2);
        verify(userService).findById(1);
    }

    @Test
    @DisplayName("findByUser: lanza excepción si el usuario no existe")
    void findByUser_throwsWhenUserNotFound() {
        when(userService.findById(99))
                .thenThrow(new ResourceNotFoundException("User", 99));

        assertThatThrownBy(() -> priceAlertService.findByUser(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceAlertRepository, never()).findByUserId(any());
    }

    // ─── findActiveByUser ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findActiveByUser: retorna solo alertas activas del usuario")
    void findActiveByUser_returnsOnlyActive() {
        when(userService.findById(1)).thenReturn(mario);
        when(priceAlertRepository.findByUserIdAndActiveTrue(1))
                .thenReturn(List.of(activeAlert));

        List<PriceAlert> result = priceAlertService.findActiveByUser(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
    }

    // ─── findActiveByProduct ──────────────────────────────────────────────────

    @Test
    @DisplayName("findActiveByProduct: valida producto y retorna alertas activas")
    void findActiveByProduct_validatesProductAndReturnsActive() {
        when(productService.findById(1)).thenReturn(notebook);
        when(priceAlertRepository.findActiveAlertsByProduct(1))
                .thenReturn(List.of(activeAlert));

        List<PriceAlert> result = priceAlertService.findActiveByProduct(1);

        assertThat(result).hasSize(1);
        verify(productService).findById(1);
    }

    @Test
    @DisplayName("findActiveByProduct: lanza excepción si el producto no existe")
    void findActiveByProduct_throwsWhenProductNotFound() {
        when(productService.findById(99))
                .thenThrow(new ResourceNotFoundException("Product", 99));

        assertThatThrownBy(() -> priceAlertService.findActiveByProduct(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: valida user y product, fuerza active=true y guarda")
    void create_setsActiveTrueAndSaves() {
        PriceAlert alert = PriceAlert.builder()
                .user(mario).product(notebook)
                .targetPrice(new BigDecimal("850.00"))
                .active(false)   // el servicio debe sobreescribir esto
                .build();

        when(userService.findById(1)).thenReturn(mario);
        when(productService.findById(1)).thenReturn(notebook);
        when(priceAlertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PriceAlert result = priceAlertService.create(alert);

        assertThat(result.getActive()).isTrue();
        verify(priceAlertRepository).save(alert);
    }

    @Test
    @DisplayName("create: lanza excepción si el usuario no existe")
    void create_throwsWhenUserNotFound() {
        mario.setId(99);
        when(userService.findById(99))
                .thenThrow(new ResourceNotFoundException("User", 99));

        assertThatThrownBy(() -> priceAlertService.create(activeAlert))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: lanza excepción si el producto no existe")
    void create_throwsWhenProductNotFound() {
        when(userService.findById(1)).thenReturn(mario);
        notebook.setId(99);
        when(productService.findById(99))
                .thenThrow(new ResourceNotFoundException("Product", 99));

        assertThatThrownBy(() -> priceAlertService.create(activeAlert))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceAlertRepository, never()).save(any());
    }

    // ─── deactivate ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate: cambia active a false y guarda")
    void deactivate_setsActiveFalseAndSaves() {
        when(priceAlertRepository.findById(1)).thenReturn(Optional.of(activeAlert));
        when(priceAlertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PriceAlert result = priceAlertService.deactivate(1);

        assertThat(result.getActive()).isFalse();
        verify(priceAlertRepository).save(activeAlert);
    }

    @Test
    @DisplayName("deactivate: lanza ResourceNotFoundException si alerta no existe")
    void deactivate_throwsWhenNotFound() {
        when(priceAlertRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceAlertService.deactivate(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceAlertRepository, never()).save(any());
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina alerta existente")
    void delete_removesAlert() {
        when(priceAlertRepository.findById(1)).thenReturn(Optional.of(activeAlert));

        priceAlertService.delete(1);

        verify(priceAlertRepository).delete(activeAlert);
    }

    @Test
    @DisplayName("delete: lanza ResourceNotFoundException si alerta no existe")
    void delete_throwsWhenNotFound() {
        when(priceAlertRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceAlertService.delete(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(priceAlertRepository, never()).delete(any());
    }
}
