package com.example.app.service;

import com.example.app.data.AppUser;
import com.example.app.data.Role;
import com.example.app.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> search(String term) {
        if (term == null || term.isBlank()) return repository.findAllOrdered();
        return repository.searchByUsername(term.trim());
    }

    public AppUser createUser(String username, String plainPassword, Role role, Boolean enabled) {
        String normalized = requireUsername(username);
        if (repository.existsByUsernameIgnoreCase(normalized)) throw new IllegalArgumentException("Der Username existiert bereits.");
        requirePassword(plainPassword);
        return repository.insert(normalized, passwordEncoder.encode(plainPassword), role == null ? Role.USER : role, enabled == null ? true : enabled);
    }

    public AppUser updateUser(Long id, String username, Role role, Boolean enabled) {
        AppUser existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Benutzer wurde nicht gefunden."));
        String normalized = requireUsername(username);
        repository.findByUsernameIgnoreCase(normalized)
                .filter(other -> !other.getId().equals(existing.getId()))
                .ifPresent(other -> { throw new IllegalArgumentException("Der Username existiert bereits."); });
        return repository.update(existing.getId(), normalized, role == null ? Role.USER : role, enabled == null ? true : enabled);
    }

    public void changePassword(Long id, String plainPassword) {
        requirePassword(plainPassword);
        repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Benutzer wurde nicht gefunden."));
        repository.updatePassword(id, passwordEncoder.encode(plainPassword));
    }

    public void deleteUser(Long id) {
        repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Benutzer wurde nicht gefunden."));
        repository.deleteById(id);
    }

    private String requireUsername(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username ist erforderlich.");
        return username.trim();
    }

    private void requirePassword(String password) {
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Passwort ist erforderlich.");
    }
}
