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
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ConfigurationService {

    private ConfigurationRepository configurationRepository;

    @Getter
    private Map<Long, HikariDataSource> activePools = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    public ConfigurationService(ConfigurationRepository configurationRepository) {

        this.configurationRepository = configurationRepository;
    };

    public List<Configuration> findAllConfigurations(){
        return configurationRepository.findAll();
    };

    //   public List<Configuration> findMessageConfigurations(){
    //       return configurationRepository.findByName();
    //   };
    public Configuration findByIdConfiguration(Long id) {
        if (id == null) {
            System.err.println("ID is null!");
            return null;
        }
        return configurationRepository.findById(id).get();
    }
    public Optional<Configuration> findById(Long id) {
        if (id == null) {
            System.err.println("ID is null!");
            return null;
        }
        return configurationRepository.findById(id);
    }

    public List<Configuration> findMessageConfigurations(){
        return configurationRepository.findAll();
    };


    public void saveConfigurationOld(Configuration config) {

        if (config == null) {
            System.err.println("Configuration is null!");
            return;
        }
        configurationRepository.save(config);
    }

    @Transactional
    public void saveConfiguration(Configuration config) {
        if (config == null) {
            System.err.println("Configuration is null!");
            return;
        }
        //  String plainTextPassword = config.getPassword();

        if (config.getId() == null) {
            // New configuration, save it and let the database generate the ID
            config.setPassword(Configuration.encodePassword(config.getPassword()));
            configurationRepository.save(config);
            System.out.println("New Configuration saved with ID: " + config.getId());
        } else {
            Optional<Configuration> existingConfigOptional = configurationRepository.findById(config.getId());

            if (existingConfigOptional.isPresent()) {
                Configuration existingConfig = existingConfigOptional.get();
                if (existingConfig != null && !existingConfig.getIsMonitoring().equals(config.getIsMonitoring())) {
                    updatePoolStatus(config);
                }

                System.out.println("password existing = " + existingConfig.getPassword());
                boolean passwordChanged = !config.getPassword().equals(existingConfig.getPassword());
                System.out.println("password new updated = " + config.getPassword());
                System.out.println("password changed = " + passwordChanged);

                if (passwordChanged) {
                    System.out.println("password changed!!!!");
                    String encodedPassword = Configuration.encodePassword(config.getPassword());
                    //  String encodedPassword = (config.getPassword());
                    configurationRepository.updateWithPassword(
                            config.getId(),
                            config.getName(),
                            config.getUserName(),
                            encodedPassword,
                            config.getDb_Url(),
                            config.getIsMonitoring(),
                            config.getIsWatchdog()
                    );
                } else {
                    System.out.println("password not changed!!!!");
                    configurationRepository.updateWithoutPassword(
                            config.getId(),
                            config.getName(),
                            config.getUserName(),
                            config.getDb_Url(),
                            config.getIsMonitoring(),
                            config.getIsWatchdog()
                    );
                }
            } else {
                // Handle case where configuration might not exist
                System.err.println("Configuration with name and ID " + config.getName() + "___"+ config.getId() + " not found!");
            }
        }
    }

    public void deleteConfiguration(Configuration config) {
        if (config.getId() == null) {
            System.err.println("ID is null!");
            return;
        }

        configurationRepository.deleteById(config.getId());
    }


    /**
     * Start HikariCP pool for a specific configuration.
     */
    public void startPool(Configuration config) {
        if (!activePools.containsKey(config.getId())) {

            HikariConfig hikariConfig = new HikariConfig();
            logger.error("Configuration username: " + config.getUserName() + " is database URL :"+config.getDb_Url());
            if (config.getDb_Url() == null || config.getDb_Url().isEmpty()) {
                logger.error("Configuration username : " + config.getUserName() + " is missing the database URL.");
                return;
            }
            hikariConfig.setJdbcUrl(config.getDb_Url());
            hikariConfig.setUsername(config.getUserName());
            hikariConfig.setPassword(Configuration.decodePassword(config.getPassword()));

            int maximumPoolSize = Application.maxPoolsizeMap.get(config.getId());
            String poolName = "CP_" + config.getName();
            hikariConfig.setPoolName(poolName);
            hikariConfig.setMaximumPoolSize(maximumPoolSize +1 );  // Adjust for your concurrency needs
//            hikariConfig.setConnectionTimeout(60000); // 60 seconds

            // Set maxLifetime to 3 minutes (180,000 ms)
//            hikariConfig.setMaxLifetime(1800000); // 3 minutes
//            hikariConfig.setIdleTimeout(1800000); // 30 minutes
            //hikariConfig.setIdleTimeout(30000); // 30 Sekunden
//            hikariConfig.setValidationTimeout(5000); // 5 seconds
//            hikariConfig.setKeepaliveTime(600000);   // 10 minutes
//            hikariConfig.setValidationTimeout(5000);

            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            activePools.put((config.getId()), dataSource);

            System.out.println("Started pool: " + poolName);
        }
    }

    /**
     * Stop HikariCP pool for a specific configuration.
     */
    public void stopPool(Long configId) {
        HikariDataSource dataSource = activePools.remove(configId);
        if (dataSource != null) {
            dataSource.close();
            System.out.println("Stopped pool for config ID: " + configId);
        }
    }


    /**
     * Update pool status based on changes in the 'Is_Monitoring' flag.
     */
    public void updatePoolStatus(Configuration config) {
        if (config.getIsMonitoring() == 1 && !activePools.containsKey(config.getId())) {
            startPool(config);
        } else if (config.getIsMonitoring() == 0 && activePools.containsKey(config.getId())) {
            stopPool(config.getId());
        }
    }
}