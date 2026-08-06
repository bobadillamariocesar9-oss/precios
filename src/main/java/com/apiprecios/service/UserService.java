package com.apiprecios.service;

import com.apiprecios.entity.User;
import com.apiprecios.exception.BadRequestException;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User con email " + email + " no encontrado"));
    }

    @Transactional
    public User create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BadRequestException("El email '" + user.getEmail() + "' ya está registrado");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BadRequestException("El username '" + user.getUsername() + "' ya está en uso");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public User update(Integer id, User data) {
        User user = findById(id);

        if (!user.getEmail().equals(data.getEmail())
                && userRepository.existsByEmail(data.getEmail())) {
            throw new BadRequestException("El email '" + data.getEmail() + "' ya está registrado");
        }
        if (!user.getUsername().equals(data.getUsername())
                && userRepository.existsByUsername(data.getUsername())) {
            throw new BadRequestException("El username '" + data.getUsername() + "' ya está en uso");
        }

        user.setUsername(data.getUsername());
        user.setEmail(data.getEmail());
        if (data.getPassword() != null && !data.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(data.getPassword()));
        }
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Integer id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
