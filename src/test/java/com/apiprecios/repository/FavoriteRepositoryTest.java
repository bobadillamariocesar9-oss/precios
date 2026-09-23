package com.apiprecios.repository;

import com.apiprecios.AbstractRepositoryTest;
import com.apiprecios.entity.Favorite;
import com.apiprecios.entity.Product;
import com.apiprecios.entity.Store;
import com.apiprecios.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FavoriteRepository")
class FavoriteRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private FavoriteRepository favoriteRepository;

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
    private Product teclado;

    @BeforeEach
    void setUp() {
        favoriteRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();

        mario = userRepository.save(User.builder()
                .username("mario").email("mario@test.com").password("pass").build());
        ana = userRepository.save(User.builder()
                .username("ana").email("ana@test.com").password("pass").build());

        Store store = storeRepository.save(Store.builder()
                .name("MercadoLibre").url("https://ml.com").build());

        notebook = productRepository.save(Product.builder()
                .name("Notebook").store(store).build());
        mouse = productRepository.save(Product.builder()
                .name("Mouse").store(store).build());
        teclado = productRepository.save(Product.builder()
                .name("Teclado").store(store).build());

        // mario tiene notebook y mouse como favoritos
        favoriteRepository.save(Favorite.builder().user(mario).product(notebook).build());
        favoriteRepository.save(Favorite.builder().user(mario).product(mouse).build());
        // ana tiene solo notebook
        favoriteRepository.save(Favorite.builder().user(ana).product(notebook).build());
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save: persiste favorito con createdAt auto-asignado")
    void save_persistsFavoriteWithCreatedAt() {
        Favorite fav = favoriteRepository.save(
                Favorite.builder().user(mario).product(teclado).build());

        assertThat(fav.getId()).isNotNull();
        assertThat(fav.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("save: falla con constraint única (user + product duplicados)")
    void save_failsOnDuplicateUserProduct() {
        Favorite duplicate = Favorite.builder().user(mario).product(notebook).build();
        assertThatThrownBy(() -> favoriteRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ─── findAll ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: retorna todos los favoritos")
    void findAll_returnsAllFavorites() {
        assertThat(favoriteRepository.findAll()).hasSize(3);
    }

    // ─── findByUserId ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserId: retorna favoritos del usuario indicado")
    void findByUserId_returnsFavoritesForUser() {
        List<Favorite> favs = favoriteRepository.findByUserId(mario.getId());
        assertThat(favs).hasSize(2);
        assertThat(favs).extracting(f -> f.getProduct().getName())
                .containsExactlyInAnyOrder("Notebook", "Mouse");
    }

    @Test
    @DisplayName("findByUserId: retorna vacío si usuario sin favoritos")
    void findByUserId_returnsEmptyForUserWithNoFavorites() {
        User nuevo = userRepository.save(User.builder()
                .username("nuevo").email("nuevo@test.com").password("pass").build());
        assertThat(favoriteRepository.findByUserId(nuevo.getId())).isEmpty();
    }

    // ─── findByUserIdAndProductId ─────────────────────────────────────────────

    @Test
    @DisplayName("findByUserIdAndProductId: retorna favorito específico")
    void findByUserIdAndProductId_returnsFavorite() {
        Optional<Favorite> fav = favoriteRepository
                .findByUserIdAndProductId(mario.getId(), notebook.getId());
        assertThat(fav).isPresent();
    }

    @Test
    @DisplayName("findByUserIdAndProductId: retorna vacío si no es favorito")
    void findByUserIdAndProductId_returnsEmptyWhenNotFavorite() {
        Optional<Favorite> fav = favoriteRepository
                .findByUserIdAndProductId(mario.getId(), teclado.getId());
        assertThat(fav).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdAndProductId: retorna vacío para usuario equivocado")
    void findByUserIdAndProductId_returnsEmptyForWrongUser() {
        Optional<Favorite> fav = favoriteRepository
                .findByUserIdAndProductId(ana.getId(), mouse.getId());
        assertThat(fav).isEmpty();
    }

    // ─── existsByUserIdAndProductId ───────────────────────────────────────────

    @Test
    @DisplayName("existsByUserIdAndProductId: true cuando el favorito existe")
    void existsByUserIdAndProductId_trueWhenExists() {
        assertThat(favoriteRepository
                .existsByUserIdAndProductId(mario.getId(), notebook.getId())).isTrue();
    }

    @Test
    @DisplayName("existsByUserIdAndProductId: false cuando no existe")
    void existsByUserIdAndProductId_falseWhenNotExists() {
        assertThat(favoriteRepository
                .existsByUserIdAndProductId(mario.getId(), teclado.getId())).isFalse();
    }

    // ─── deleteByUserIdAndProductId ───────────────────────────────────────────

    @Test
    @DisplayName("deleteByUserIdAndProductId: elimina el favorito correcto")
    void deleteByUserIdAndProductId_removesCorrectFavorite() {
        favoriteRepository.deleteByUserIdAndProductId(mario.getId(), notebook.getId());

        assertThat(favoriteRepository
                .existsByUserIdAndProductId(mario.getId(), notebook.getId())).isFalse();
        assertThat(favoriteRepository
                .existsByUserIdAndProductId(ana.getId(), notebook.getId())).isTrue();
        assertThat(favoriteRepository
                .existsByUserIdAndProductId(mario.getId(), mouse.getId())).isTrue();
    }

    @Test
    @DisplayName("deleteByUserIdAndProductId: no lanza error si no existe")
    void deleteByUserIdAndProductId_noErrorWhenNotExists() {
        assertThatCode(() ->
                favoriteRepository.deleteByUserIdAndProductId(mario.getId(), teclado.getId())
        ).doesNotThrowAnyException();
    }
}
