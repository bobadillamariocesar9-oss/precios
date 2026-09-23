package com.apiprecios.repository;

import com.apiprecios.AbstractRepositoryTest;
import com.apiprecios.entity.PriceAlert;
import com.apiprecios.entity.Product;
import com.apiprecios.entity.Store;
import com.apiprecios.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PriceAlertRepository")
class PriceAlertRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    private User mario;
    private User ana;
    private Product notebook;
    private Product mouse;

    @BeforeEach
    void setUp() {
        priceAlertRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();

        mario = userRepository.save(User.builder()
                .username("mario").email("mario@test.com").password("pass").build());
        ana = userRepository.save(User.builder()
                .username("ana").email("ana@test.com").password("pass").build());

        Store store = storeRepository.save(Store.builder()
                .name("MercadoLibre").baseUrl("https://ml.com").build());

        notebook = productRepository.save(Product.builder()
                .name("Notebook").store(store).build());
        mouse = productRepository.save(Product.builder()
                .name("Mouse").store(store).build());

        // mario: 2 alertas activas para notebook, 1 inactiva para mouse
        priceAlertRepository.save(PriceAlert.builder()
                .user(mario).product(notebook)
                .targetPrice(new BigDecimal("900.00")).active(true).build());
        priceAlertRepository.save(PriceAlert.builder()
                .user(mario).product(notebook)
                .targetPrice(new BigDecimal("800.00")).active(true).build());
        priceAlertRepository.save(PriceAlert.builder()
                .user(mario).product(mouse)
                .targetPrice(new BigDecimal("40.00")).active(false).build());

        // ana: 1 alerta activa para notebook
        priceAlertRepository.save(PriceAlert.builder()
                .user(ana).product(notebook)
                .targetPrice(new BigDecimal("850.00")).active(true).build());
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save: persiste alerta con createdAt auto-asignado")
    void save_persistsAlertWithCreatedAt() {
        PriceAlert alert = priceAlertRepository.save(PriceAlert.builder()
                .user(ana).product(mouse)
                .targetPrice(new BigDecimal("35.00"))
                .active(true)
                .build());

        assertThat(alert.getId()).isNotNull();
        assertThat(alert.getCreatedAt()).isNotNull();
        assertThat(alert.getActive()).isTrue();
    }

    // ─── findByUserId ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserId: retorna todas las alertas del usuario (activas e inactivas)")
    void findByUserId_returnsAllAlertsForUser() {
        List<PriceAlert> alerts = priceAlertRepository.findByUserId(mario.getId());
        assertThat(alerts).hasSize(3);
    }

    @Test
    @DisplayName("findByUserId: retorna vacío si usuario sin alertas")
    void findByUserId_returnsEmptyForUserWithNoAlerts() {
        User nuevo = userRepository.save(User.builder()
                .username("nuevo").email("nuevo@test.com").password("pass").build());
        assertThat(priceAlertRepository.findByUserId(nuevo.getId())).isEmpty();
    }

    // ─── findByUserIdAndActiveTrue ────────────────────────────────────────────

    @Test
    @DisplayName("findByUserIdAndActiveTrue: retorna solo alertas activas del usuario")
    void findByUserIdAndActiveTrue_returnsOnlyActiveAlerts() {
        List<PriceAlert> active = priceAlertRepository
                .findByUserIdAndActiveTrue(mario.getId());

        assertThat(active).hasSize(2);
        assertThat(active).allMatch(PriceAlert::getActive);
    }

    @Test
    @DisplayName("findByUserIdAndActiveTrue: retorna vacío si usuario solo tiene alertas inactivas")
    void findByUserIdAndActiveTrue_returnsEmptyWhenAllInactive() {
        // Desactivamos todas las de mario
        priceAlertRepository.findByUserId(mario.getId()).forEach(a -> {
            a.setActive(false);
            priceAlertRepository.save(a);
        });
        assertThat(priceAlertRepository.findByUserIdAndActiveTrue(mario.getId())).isEmpty();
    }

    // ─── findByProductIdAndActiveTrue ────────────────────────────────────────

    @Test
    @DisplayName("findByProductIdAndActiveTrue: retorna alertas activas para un producto")
    void findByProductIdAndActiveTrue_returnsActiveAlertsForProduct() {
        List<PriceAlert> active = priceAlertRepository
                .findByProductIdAndActiveTrue(notebook.getId());

        // mario tiene 2 activas + ana tiene 1 activa para notebook
        assertThat(active).hasSize(3);
        assertThat(active).allMatch(PriceAlert::getActive);
    }

    @Test
    @DisplayName("findByProductIdAndActiveTrue: retorna vacío si producto sin alertas activas")
    void findByProductIdAndActiveTrue_returnsEmptyWhenNoActiveAlerts() {
        // Mouse solo tiene 1 alerta, y está inactiva
        List<PriceAlert> active = priceAlertRepository
                .findByProductIdAndActiveTrue(mouse.getId());
        assertThat(active).isEmpty();
    }

    // ─── findActiveAlertsByProduct (@Query) ───────────────────────────────────

    @Test
    @DisplayName("findActiveAlertsByProduct: query JPQL retorna solo activas")
    void findActiveAlertsByProduct_returnsOnlyActive() {
        List<PriceAlert> active = priceAlertRepository
                .findActiveAlertsByProduct(notebook.getId());
        assertThat(active).hasSize(3);
        assertThat(active).allMatch(PriceAlert::getActive);
    }

    @Test
    @DisplayName("findActiveAlertsByProduct: coherente con findByProductIdAndActiveTrue")
    void findActiveAlertsByProduct_consistentWithDerivedQuery() {
        List<PriceAlert> byQuery = priceAlertRepository
                .findActiveAlertsByProduct(notebook.getId());
        List<PriceAlert> byDerived = priceAlertRepository
                .findByProductIdAndActiveTrue(notebook.getId());

        assertThat(byQuery).hasSameSizeAs(byDerived);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina una alerta")
    void delete_removesAlert() {
        long before = priceAlertRepository.count();
        PriceAlert toDelete = priceAlertRepository
                .findByUserId(mario.getId()).get(0);
        priceAlertRepository.delete(toDelete);
        assertThat(priceAlertRepository.count()).isEqualTo(before - 1);
    }
}
