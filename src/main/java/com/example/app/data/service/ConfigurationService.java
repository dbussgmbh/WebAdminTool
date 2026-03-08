package com.example.app.data.service;

import com.example.app.Application;
import com.example.app.data.entity.Configuration;
import com.example.app.data.repository.ConfigurationRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.transaction.Transactional;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ConfigurationService {

    private final ConfigurationRepository configurationRepository;

    @Getter
    private final Map<Long, HikariDataSource> activePools = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    @Value("${app.aes.secret:${APP_AES_SECRET:}}")
    private String aesSecret;

    public ConfigurationService(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public List<Configuration> findAllConfigurations() {
        return configurationRepository.findAll();
    }

    public Configuration findByIdConfiguration(Long id) {
        if (id == null) {
            logger.error("ID is null");
            return null;
        }
        return configurationRepository.findById(id).orElse(null);
    }

    public Optional<Configuration> findById(Long id) {
        if (id == null) {
            logger.error("ID is null");
            return Optional.empty();
        }
        return configurationRepository.findById(id);
    }

    public List<Configuration> findMessageConfigurations() {
        return configurationRepository.findAll();
    }

    public void saveConfigurationOld(Configuration config) {
        if (config == null) {
            logger.error("Configuration is null");
            return;
        }
        configurationRepository.save(config);
    }

    @Transactional
    public void saveConfiguration(Configuration config) {
        if (config == null) {
            logger.error("Configuration is null");
            return;
        }

        if (config.getId() == null) {
            String encryptedPassword = encryptIfNecessary(config.getPassword());
            config.setPassword(encryptedPassword);
            configurationRepository.save(config);
            logger.info("New Configuration saved with ID: {}", config.getId());
            return;
        }

        Optional<Configuration> existingConfigOptional = configurationRepository.findById(config.getId());

        if (existingConfigOptional.isEmpty()) {
            logger.error("Configuration with ID {} not found", config.getId());
            return;
        }

        Configuration existingConfig = existingConfigOptional.get();

        if (existingConfig.getIsMonitoring() != null
                && config.getIsMonitoring() != null
                && !existingConfig.getIsMonitoring().equals(config.getIsMonitoring())) {
            updatePoolStatus(config);
        }

        boolean passwordChanged = !safe(config.getPassword()).equals(safe(existingConfig.getPassword()));

        logger.info("Updating configuration {}, passwordChanged={}", config.getId(), passwordChanged);

        if (passwordChanged) {
            String encryptedPassword = encryptIfNecessary(config.getPassword());

            configurationRepository.updateWithPassword(
                    config.getId(),
                    config.getName(),
                    config.getUserName(),
                    encryptedPassword,
                    config.getDb_Url(),
                    config.getIsMonitoring(),
                    config.getIsWatchdog()
            );

            config.setPassword(encryptedPassword);
        } else {
            configurationRepository.updateWithoutPassword(
                    config.getId(),
                    config.getName(),
                    config.getUserName(),
                    config.getDb_Url(),
                    config.getIsMonitoring(),
                    config.getIsWatchdog()
            );
        }
    }

    public void deleteConfiguration(Configuration config) {
        if (config == null || config.getId() == null) {
            logger.error("Configuration or ID is null");
            return;
        }

        configurationRepository.deleteById(config.getId());
    }

    /**
     * Start HikariCP pool for a specific configuration.
     */
    public void startPool(Configuration config) {
        if (config == null || config.getId() == null) {
            logger.error("Cannot start pool: configuration or ID is null");
            return;
        }

        if (activePools.containsKey(config.getId())) {
            return;
        }

        if (config.getDb_Url() == null || config.getDb_Url().isBlank()) {
            logger.error("Configuration {} is missing database URL", config.getUserName());
            return;
        }

        try {
            String plainPassword = getPlainPasswordAndMigrateIfNeeded(config);

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getDb_Url());
            hikariConfig.setUsername(config.getUserName());
            hikariConfig.setPassword(plainPassword);

            int maximumPoolSize = Application.maxPoolsizeMap.getOrDefault(config.getId(), 5);
            String poolName = "CP_" + config.getName();

            hikariConfig.setPoolName(poolName);
            hikariConfig.setMaximumPoolSize(maximumPoolSize + 1);

            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            activePools.put(config.getId(), dataSource);

            logger.info("Started pool: {}", poolName);
        } catch (Exception e) {
            logger.error("Error starting pool for config {}: {}", config.getId(), e.getMessage(), e);
        }
    }

    /**
     * Stop HikariCP pool for a specific configuration.
     */
    public void stopPool(Long configId) {
        HikariDataSource dataSource = activePools.remove(configId);
        if (dataSource != null) {
            dataSource.close();
            logger.info("Stopped pool for config ID: {}", configId);
        }
    }

    /**
     * Update pool status based on changes in the 'Is_Monitoring' flag.
     */
    public void updatePoolStatus(Configuration config) {
        if (config == null || config.getId() == null) {
            return;
        }

        if (config.getIsMonitoring() != null && config.getIsMonitoring() == 1 && !activePools.containsKey(config.getId())) {
            startPool(config);
        } else if (config.getIsMonitoring() != null && config.getIsMonitoring() == 0 && activePools.containsKey(config.getId())) {
            stopPool(config.getId());
        }
    }

    /**
     * Returns plaintext password.
     * If a plaintext password is found in DB, it is automatically encrypted and stored back.
     */
    public String getPlainPasswordAndMigrateIfNeeded(Configuration config) {
        if (config == null) {
            return null;
        }

        String storedPassword = config.getPassword();

        if (storedPassword == null || storedPassword.isBlank()) {
            return storedPassword;
        }

        String pw="";
        if (Configuration.isEncryptedPassword(storedPassword)) {
            try{
                pw=Configuration.decodePassword(storedPassword, aesSecret);
            }
            catch(Exception e){
                logger.error(e.getMessage());
                return pw;
            }

            return pw;
        }

        logger.warn("Plaintext password found for configuration ID {}. Migrating to AES encryption.", config.getId());

        String plainPassword = storedPassword;
        String encryptedPassword = Configuration.encryptPassword(plainPassword, aesSecret);

        config.setPassword(encryptedPassword);
        persistEncryptedPassword(config.getId(), encryptedPassword);

        return plainPassword;
    }

    private void persistEncryptedPassword(Long id, String encryptedPassword) {
        Optional<Configuration> optional = configurationRepository.findById(id);
        if (optional.isEmpty()) {
            logger.error("Could not persist encrypted password. Configuration {} not found.", id);
            return;
        }

        Configuration existing = optional.get();

        configurationRepository.updateWithPassword(
                id,
                existing.getName(),
                existing.getUserName(),
                encryptedPassword,
                existing.getDb_Url(),
                existing.getIsMonitoring(),
                existing.getIsWatchdog()
        );

        logger.info("Password for configuration {} migrated to AES.", id);
    }

    private String encryptIfNecessary(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        if (Configuration.isEncryptedPassword(value)) {
            return value;
        }

        return Configuration.encryptPassword(value, aesSecret);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}