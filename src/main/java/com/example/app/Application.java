package com.example.app;

import com.example.app.data.entity.Configuration;
import com.example.app.data.service.ConfigurationService;
import com.example.app.service.CockpitService;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Push
@Theme("my-theme")
@SpringBootApplication
public class Application implements AppShellConfigurator {

    public static HashMap<Long, Integer> maxPoolsizeMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CockpitService cockpitService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        initializePools();

    }

    private void initializePools() {
        logger.info("initializePools(): initialize confifuration hikari-pools");
        List<Configuration> configurations = configurationService.findMessageConfigurations();

        for (Configuration config : configurations) {
            int maximumPoolSize  = cockpitService.fetchMaxParallel(config);
            maximumPoolSize = ( maximumPoolSize > 0) ? maximumPoolSize : 1;
            maxPoolsizeMap.put(config.getId(), maximumPoolSize);
            managePoolForConfiguration(config);
        }
        System.out.println("Count Hikari Pools: " + configurationService.getActivePools().size());
        for (Map.Entry<Long, HikariDataSource> entry : configurationService.getActivePools().entrySet()) {
            HikariDataSource dataSource = entry.getValue();
            String poolName = dataSource.getPoolName();
            System.out.println("Pool ID: " + entry.getKey() + ", Pool Name: " + poolName);
        }
    }

    /**
     * Start or stop a HikariCP connection pool based on the 'Is_Monitoring' flag.
     */
    public void managePoolForConfiguration(Configuration config) {
        logger.info("managePoolForConfiguration(Configuration config) : manage pool of "+ config.getUserName());
        if (config.getIsMonitoring() == 1) {
            configurationService.startPool(config);
        } else {
            configurationService.stopPool(config.getId());
        }
    }

}