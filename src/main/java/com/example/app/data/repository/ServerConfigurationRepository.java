package com.example.app.data.repository;

import com.example.app.data.entity.ServerConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServerConfigurationRepository extends JpaRepository<ServerConfiguration, Long> {
    Optional<ServerConfiguration> findByHostNameAndUserName(String hostName, String userName);
}
