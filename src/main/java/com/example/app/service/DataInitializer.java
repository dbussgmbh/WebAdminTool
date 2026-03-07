package com.example.app.service;

import com.example.app.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner resetPasswords(AppUserRepository repo, PasswordEncoder encoder) {
        return args -> {
            repo.updatePasswordByUsername("admin", encoder.encode("admin"));
            repo.updatePasswordByUsername("user", encoder.encode("user"));
        };
    }
}