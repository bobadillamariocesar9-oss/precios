package com.apiprecios.service;

import com.apiprecios.entity.User;
import com.apiprecios.exception.BadRequestException;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User mario;

    @BeforeEach
    void setUp() {
        mario = User.builder()
                .username("mario")
                .email("mario@test.com")
                .password("hashed")
                .build();
        mario = setId(mario, 1);
    }

    // ─── findAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: delega al repositorio y retorna la lista")
    void findAll_delegatesToRepository() {
        when(userRepository.findAll()).thenReturn(List.of(mario));

        List<User> result = userService.findAll();

        assertThat(result).containsExactly(mario);
        verify(userRepository).findAll();
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: retorna usuario cuando existe")
    void findById_returnsUserWhenFound() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mario));

        User result = userService.findById(1);

        assertThat(result).isEqualTo(mario);
    }

    @Test
    @DisplayName("findById: lanza ResourceNotFoundException cuando no existe")
    void findById_throwsWhenNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── findByEmail ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByEmail: retorna usuario cuando el email existe")
    void findByEmail_returnsUser() {
        when(userRepository.findByEmail("mario@test.com")).thenReturn(Optional.of(mario));

        User result = userService.findByEmail("mario@test.com");

        assertThat(result.getEmail()).isEqualTo("mario@test.com");
    }

    @Test
    @DisplayName("findByEmail: lanza ResourceNotFoundException cuando no existe")
    void findByEmail_throwsWhenNotFound() {
        when(userRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("x@x.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("x@x.com");
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: guarda y retorna el usuario cuando email y username son únicos")
    void create_savesAndReturnsUser() {
        when(userRepository.existsByEmail("mario@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("mario")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$argon2id$hashed");
        when(userRepository.save(any())).thenReturn(mario);

        User result = userService.create(mario);

        assertThat(result).isEqualTo(mario);
        verify(passwordEncoder).encode("hashed");
        verify(userRepository).save(mario);
    }

    @Test
    @DisplayName("create: la contraseña se guarda hasheada con Argon2")
    void create_passwordIsHashed() {
        User nuevo = User.builder()
                .username("nuevo")
                .email("nuevo@test.com")
                .password("plain123")
                .build();

        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("nuevo")).thenReturn(false);
        when(passwordEncoder.encode("plain123")).thenReturn("$argon2id$v=19$m=65536,t=3,p=1$...");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.create(nuevo);

        assertThat(result.getPassword()).startsWith("$argon2id$");
        assertThat(result.getPassword()).isNotEqualTo("plain123");
        verify(passwordEncoder).encode("plain123");
    }

    @Test
    @DisplayName("update: la nueva contraseña se hashea si se proporciona")
    void update_newPasswordIsHashed() {
        User data = User.builder()
                .username("mario")
                .email("mario@test.com")
                .password("newplain")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(mario));
        when(passwordEncoder.encode("newplain")).thenReturn("$argon2id$newhash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.update(1, data);

        assertThat(result.getPassword()).isEqualTo("$argon2id$newhash");
        verify(passwordEncoder).encode("newplain");
    }

    @Test
    @DisplayName("update: no hashea si no se proporciona nueva contraseña")
    void update_skipsHashWhenNoPassword() {
        User data = User.builder()
                .username("mario")
                .email("mario@test.com")
                .password("")   // vacío → no se re-hashea
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(mario));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.update(1, data);

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("create: lanza BadRequestException si el email ya existe")
    void create_throwsWhenEmailDuplicated() {
        when(userRepository.existsByEmail("mario@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(mario))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mario@test.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: lanza BadRequestException si el username ya existe")
    void create_throwsWhenUsernameDuplicated() {
        when(userRepository.existsByEmail("mario@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("mario")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(mario))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mario");

        verify(userRepository, never()).save(any());
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: actualiza campos y retorna usuario guardado")
    void update_updatesFieldsAndSaves() {
        User data = User.builder()
                .username("mario_v2")
                .email("mario_v2@test.com")
                .password("new_hash")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(mario));
        when(userRepository.existsByEmail("mario_v2@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("mario_v2")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.update(1, data);

        assertThat(result.getUsername()).isEqualTo("mario_v2");
        assertThat(result.getEmail()).isEqualTo("mario_v2@test.com");
        verify(userRepository).save(mario);
    }

    @Test
    @DisplayName("update: no valida unicidad si email/username no cambian")
    void update_skipsValidationWhenEmailUnchanged() {
        User data = User.builder()
                .username("mario")           // mismo username
                .email("mario@test.com")     // mismo email
                .password("new_hash")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(mario));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.update(1, data);

        // No debe verificar existencia si no cambian
        verify(userRepository, never()).existsByEmail("mario@test.com");
        verify(userRepository, never()).existsByUsername("mario");
    }

    @Test
    @DisplayName("update: lanza BadRequestException si nuevo email ya está en uso")
    void update_throwsWhenNewEmailConflicts() {
        User data = User.builder()
                .username("mario")
                .email("taken@test.com")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(mario));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1, data))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("taken@test.com");
    }

    @Test
    @DisplayName("update: lanza ResourceNotFoundException si usuario no existe")
    void update_throwsWhenUserNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99, mario))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina el usuario cuando existe")
    void delete_removesUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mario));

        userService.delete(1);

        verify(userRepository).delete(mario);
    }

    @Test
    @DisplayName("delete: lanza ResourceNotFoundException cuando no existe")
    void delete_throwsWhenNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }

    // ─── existsByEmail ────────────────────────────────────────────────────────

    @Test
    @DisplayName("existsByEmail: delega al repositorio")
    void existsByEmail_delegatesToRepository() {
        when(userRepository.existsByEmail("mario@test.com")).thenReturn(true);

        assertThat(userService.existsByEmail("mario@test.com")).isTrue();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /** Asigna un ID sin necesidad de persistencia. */
    private static User setId(User user, Integer id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }
}
