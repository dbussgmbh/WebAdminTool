package com.example.app.service;

import com.example.app.data.entity.Configuration;
import com.example.app.data.entity.MonitorAlerting;
import com.example.app.data.entity.fvm_monitoring;
import com.example.app.data.service.ConfigurationService;
import com.example.app.views.CockpitView;
import com.vaadin.flow.component.notification.Notification;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CockpitService {

    private JdbcTemplate jdbcTemplate;
    private final ConfigurationService configurationService;

    private static final Logger logger = LoggerFactory.getLogger(CockpitService.class);

    public CockpitService(JdbcTemplate jdbcTemplate, ConfigurationService configurationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.configurationService = configurationService;
    }

    private void connectWithDatabase(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());

        String plainPassword = configurationService.getPlainPasswordAndMigrateIfNeeded(conf);
        ds.setPassword(plainPassword);

        try {
            this.jdbcTemplate = new JdbcTemplate(ds);
        } catch (Exception e) {
            logger.error("Error while connecting database {}", conf.getName(), e);
        }
    }

    public JdbcTemplate getJdbcTemplateWithDBConnetion(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        logger.info("Entschlüssel Passwort für Conf: " + conf.getName() + " (User " + conf.getUserName() + ")");

        String plainPassword = configurationService.getPlainPasswordAndMigrateIfNeeded(conf);
        ds.setPassword(plainPassword);

        try {
            this.jdbcTemplate = new JdbcTemplate(ds);
            return this.jdbcTemplate;
        } catch (Exception e) {
            logger.error("Error while connect database: {}", conf.getName(), e);
        }
        return null;
    }

    public JdbcTemplate getNewJdbcTemplateWithDatabaseold(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());

        String plainPassword = configurationService.getPlainPasswordAndMigrateIfNeeded(conf);
        ds.setPassword(plainPassword);

        try {
            return new JdbcTemplate(ds);
        } catch (Exception e) {
            logger.error("Error while creating JdbcTemplate for {}", conf.getName(), e);
        }
        logger.error("Fehler in CockpitService bei getActivePools für DB: {}",conf.getName());
        return null;
    }

    public JdbcTemplate getNewJdbcTemplateWithDatabase(Configuration conf) {
        try {
            HikariDataSource pool = configurationService.getActivePools().get(conf.getId());

            if (pool == null) {
                logger.warn("No active pool found for config {}. Starting pool now.", conf.getId());
                configurationService.startPool(conf);
                pool = configurationService.getActivePools().get(conf.getId());
            }

            if (pool == null) {
                logger.error("Pool could not be created for config {}", conf.getId());
                return null;
            }

            return new JdbcTemplate(pool);
        } catch (Exception e) {
            logger.error("Error while getting pool for config {}", conf.getName(), e);
        }
        return null;
    }

    public JdbcTemplate getNewJdbcTemplateWithDatabasenew(Configuration conf) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(conf.getDb_Url());
        hikariConfig.setUsername(conf.getUserName());

        String plainPassword = configurationService.getPlainPasswordAndMigrateIfNeeded(conf);
        hikariConfig.setPassword(plainPassword);

        hikariConfig.setMaxLifetime(30000);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("DB-pool");
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(30000);

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        return new JdbcTemplate(dataSource);
    }

    public void connectionClose() {
        // bei gepoolten Verbindungen bewusst leer
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        // bei gepoolten Verbindungen bewusst leer
    }

    public List<fvm_monitoring> getMonitoring(Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

        String sql = "SELECT m.ID,m.PID, m.Bereich, RETENTIONTIME, SQL, TYPE, SHELL_SERVER, SHELL_COMMAND, TITEL, BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL, WARNING_SCHWELLWERT"
                + ", ERROR_SCHWELLWERT, mr.result as Aktueller_Wert, 100 / Error_schwellwert * case when mr.result>=Error_schwellwert then Error_Schwellwert else mr.result end / 100 as Error_Prozent"
                + ", Zeitpunkt, m.is_active, m.sql_detail as sql_detail FROM FVM_MONITORING m "
                + "left outer join FVM_MONITOR_RESULT mr on m.id=mr.id and mr.is_active='1'";

        List<fvm_monitoring> fvmMonitorings = new ArrayList<>();
        try {
            fvmMonitorings = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(fvm_monitoring.class));
        } catch (Exception e) {
            logger.error("Exception in getMonitoring: {}", e.getMessage(), e);
        } finally {
            connectionClose(jdbcTemplate);
        }

        return fvmMonitorings;
    }

    private boolean tableExists(String tableName, String databaseType, JdbcTemplate jdbcTemplate) {
        try {
            String checkTableSql;
            switch (databaseType.toLowerCase()) {
                case "oracle":
                    checkTableSql = "SELECT COUNT(*) FROM user_tables WHERE UPPER(table_name) = UPPER(?)";
                    break;
                case "sqlserver":
                    checkTableSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported database type: " + databaseType);
            }

            int tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName.toUpperCase());
            return tableCount > 0;
        } catch (Exception e) {
            logger.error("Exception while checking table existence: {}", e.getMessage(), e);
            return false;
        }
    }

    public void createFvmMonitorAlertingTable(Configuration configuration) {
        String tableName = "FVM_MONITOR_ALERTING";
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

        if (jdbcTemplate == null) {
            logger.error("jdbcTemplate konnte nicht initialisiert werden für User {}", configuration.getUserName());
            return;
        }

        try {
            String dbType = "oracle";
            if (configuration.getName().contains("SQLServer")) {
                dbType = "sqlserver";
            }

            logger.debug("DB: {}", configuration.getName());

            logger.info("Check ob Tabelle {} bereits vorhanden ist...", tableName);
            if (!tableExists(tableName, dbType, jdbcTemplate)) {
                logger.info("Creating table: {}", tableName);

                String createTableSQL = "CREATE TABLE FVM_MONITOR_ALERTING ("
                        + "MAIL_EMPFAENGER VARCHAR(255), "
                        + "MAIL_CC_EMPFAENGER VARCHAR(255), "
                        + "MAIL_BETREFF VARCHAR(255), "
                        + "MAIL_TEXT VARCHAR(255), "
                        + "WATCHDOG_MAIL_EMPFAENGER VARCHAR(255), "
                        + "WATCHDOG_MAIL_CC_EMPFAENGER VARCHAR(255), "
                        + "WATCHDOG_MAIL_BETREFF VARCHAR(255), "
                        + "WATCHDOG_MAIL_TEXT VARCHAR(255), "
                        + "BG_JOB_CRON_EXPRESSION VARCHAR(255), "
                        + "LAST_ALERT_TIME DATE, "
                        + "LAST_ALERT_CHECKTIME TIMESTAMP, "
                        + "IS_ACTIVE INT, "
                        + "RETENTION_TIME INT, "
                        + "MAX_PARALLEL_CHECKS INT, "
                        + "ISBACKJOBACTIVE INT, "
                        + "ISMBWATCHDOGACTIVE INT, "
                        + "SIMULATION INT, "
                        + "MB_WATCHDOG_CRON_EXPRESSION VARCHAR(255) "
                        + ")";

                jdbcTemplate.execute(createTableSQL);

                String insertRowSQL = "INSERT INTO FVM_MONITOR_ALERTING ("
                        + "MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, BG_JOB_CRON_EXPRESSION, "
                        + "LAST_ALERT_TIME, LAST_ALERT_CHECKTIME, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS, "
                        + "ISBACKJOBACTIVE, ISMBWATCHDOGACTIVE, MB_WATCHDOG_CRON_EXPRESSION, WATCHDOG_MAIL_EMPFAENGER, WATCHDOG_MAIL_CC_EMPFAENGER, WATCHDOG_MAIL_BETREFF, WATCHDOG_MAIL_TEXT, SIMULATION) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(insertRowSQL,
                        "m.quaschny@t-online.de", "", "In der EKP sind Probleme", "In der EKP sind Probleme",
                        "0 0/1 * * * ?", null, null, 0, 5, 1, 1, 0, "0 0/2 * * * ?",
                        "m.quaschny@t-online.de", "", "watchdog for ekp", "In der EKP sind Probleme watchdog", 0);

                logger.info("Default row inserted successfully.");
            } else {
                logger.debug("Table already exists: {}", tableName);
            }
        } catch (Exception e) {
            logger.error("Exception: {}", e.getMessage(), e);
        } finally {
            connectionClose(jdbcTemplate);
        }
    }

    public String getCurrentSchema() {
        return jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
            String schema = null;
            try {
                schema = connection.getSchema();

                if (schema == null) {
                    DatabaseMetaData metaData = connection.getMetaData();
                    schema = metaData.getUserName();
                }

                logger.info("Current Schema: {}", schema);
            } catch (SQLException e) {
                logger.error("Error reading schema", e);
            }
            return schema;
        });
    }

    public int fetchMaxParallel(Configuration configuration) {
        int maxParallel = 0;
        JdbcTemplate jdbcTemplate = getJdbcTemplateWithDBConnetion(configuration);
        try {
            String sql = "SELECT MAX_PARALLEL_CHECKS FROM FVM_MONITOR_ALERTING";
            logger.info("Ausführen SQL: " + sql);
            logger.info("In Verbindung: " + configuration.getName());
            maxParallel = jdbcTemplate.queryForObject(sql, Integer.class);
            logger.info("fetchMaxParallel= {}", maxParallel);
            return maxParallel;
        } catch (Exception e) {
            logger.error("fetchMaxParallel ist leider fehlgeschlagen: {}", e.getMessage(), e);
        } finally {
            connectionClose(jdbcTemplate);
        }
        return 1;
    }

    public MonitorAlerting fetchEmailConfiguration(Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

        if (jdbcTemplate == null) {
            logger.error("jdbcTemplate konnte nicht initialisiert werden für User {}", configuration.getUserName());
            return null;
        }

        MonitorAlerting monitorAlerting = new MonitorAlerting();
        try {
            logger.info("fetchEmailConfiguration for {}", configuration.getName());

            String sql = "SELECT MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, WATCHDOG_MAIL_EMPFAENGER, "
                    + "WATCHDOG_MAIL_CC_EMPFAENGER, WATCHDOG_MAIL_BETREFF, WATCHDOG_MAIL_TEXT, BG_JOB_CRON_EXPRESSION, "
                    + "MB_WATCHDOG_CRON_EXPRESSION, LAST_ALERT_TIME, LAST_ALERT_CHECKTIME, IS_ACTIVE, RETENTION_TIME, "
                    + "MAX_PARALLEL_CHECKS, ISBACKJOBACTIVE, ISMBWATCHDOGACTIVE, SIMULATION FROM FVM_MONITOR_ALERTING";

            jdbcTemplate.query(sql, rs -> {
                monitorAlerting.setMailEmpfaenger(rs.getString("MAIL_EMPFAENGER"));
                monitorAlerting.setMailCCEmpfaenger(rs.getString("MAIL_CC_EMPFAENGER"));
                monitorAlerting.setMailBetreff(rs.getString("MAIL_BETREFF"));
                monitorAlerting.setMailText(rs.getString("MAIL_TEXT"));
                monitorAlerting.setWatchdogMailEmpfaenger(rs.getString("WATCHDOG_MAIL_EMPFAENGER"));
                monitorAlerting.setWatchdogMailCCEmpfaenger(rs.getString("WATCHDOG_MAIL_CC_EMPFAENGER"));
                monitorAlerting.setWatchdogMailBetreff(rs.getString("WATCHDOG_MAIL_BETREFF"));
                monitorAlerting.setWatchdogMailText(rs.getString("WATCHDOG_MAIL_TEXT"));
                monitorAlerting.setBgCron(rs.getString("BG_JOB_CRON_EXPRESSION"));
                monitorAlerting.setMbWatchdogCron(rs.getString("MB_WATCHDOG_CRON_EXPRESSION"));
                monitorAlerting.setRetentionTime(rs.getInt("RETENTION_TIME"));
                monitorAlerting.setMaxParallelCheck(rs.getInt("MAX_PARALLEL_CHECKS"));

                Timestamp lastAlertTimeStamp = rs.getTimestamp("LAST_ALERT_TIME");
                if (lastAlertTimeStamp != null) {
                    monitorAlerting.setLastAlertTime(lastAlertTimeStamp.toLocalDateTime());
                }

                Timestamp lastAlertCheckTimeStamp = rs.getTimestamp("LAST_ALERT_CHECKTIME");
                if (lastAlertCheckTimeStamp != null) {
                    monitorAlerting.setLastALertCheckTime(lastAlertCheckTimeStamp.toLocalDateTime());
                }

                monitorAlerting.setIsActive(rs.getInt("IS_ACTIVE"));
                monitorAlerting.setIsBackJobActive(rs.getInt("ISBACKJOBACTIVE"));
                monitorAlerting.setIsMBWatchdogActive(rs.getInt("ISMBWATCHDOGACTIVE"));
                monitorAlerting.setSimulation(rs.getInt("SIMULATION"));
            });

            return monitorAlerting;
        } catch (Exception e) {
            logger.error("Failed to load configuration: {}", e.getMessage(), e);
        } finally {
            connectionClose(jdbcTemplate);
        }
        return null;
    }

    public boolean saveEmailConfiguration(MonitorAlerting monitorAlerting, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            String checkQuery = "SELECT COUNT(*) FROM FVM_MONITOR_ALERTING";
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class);

            if (count != null && count > 0) {
                String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET "
                        + "MAIL_EMPFAENGER = ?, "
                        + "MAIL_CC_EMPFAENGER = ?, "
                        + "MAIL_BETREFF = ?, "
                        + "MAIL_TEXT = ?, "
                        + "IS_ACTIVE = ?";

                int rowsAffected = jdbcTemplate.update(updateQuery,
                        monitorAlerting.getMailEmpfaenger(),
                        monitorAlerting.getMailCCEmpfaenger(),
                        monitorAlerting.getMailBetreff(),
                        monitorAlerting.getMailText(),
                        monitorAlerting.getIsActive());

                return rowsAffected > 0;
            } else {
                String insertQuery = "INSERT INTO FVM_MONITOR_ALERTING "
                        + "(MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, BG_JOB_CRON_EXPRESSION, MB_WATCHDOG_CRON_EXPRESSION, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                int rowsAffected = jdbcTemplate.update(insertQuery,
                        monitorAlerting.getMailEmpfaenger(),
                        monitorAlerting.getMailCCEmpfaenger(),
                        monitorAlerting.getMailBetreff(),
                        monitorAlerting.getMailText(),
                        monitorAlerting.getBgCron(),
                        monitorAlerting.getMbWatchdogCron(),
                        monitorAlerting.getIsActive(),
                        monitorAlerting.getRetentionTime(),
                        monitorAlerting.getMaxParallelCheck());

                return rowsAffected > 0;
            }
        } catch (Exception e) {
            logger.error("Failed to save email configuration", e);
            Notification.show("Failed to save configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            connectionClose(jdbcTemplate);
        }
        return false;
    }

    public boolean saveBackgoundJobConfiguration(MonitorAlerting monitorAlerting, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            String checkQuery = "SELECT COUNT(*) FROM FVM_MONITOR_ALERTING";
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class);

            if (count != null && count > 0) {
                String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET "
                        + "BG_JOB_CRON_EXPRESSION = ?, "
                        + "ISBACKJOBACTIVE = ?, "
                        + "RETENTION_TIME = ?, "
                        + "MAX_PARALLEL_CHECKS = ?";

                int rowsAffected = jdbcTemplate.update(updateQuery,
                        monitorAlerting.getBgCron(),
                        monitorAlerting.getIsBackJobActive(),
                        monitorAlerting.getRetentionTime(),
                        monitorAlerting.getMaxParallelCheck());

                return rowsAffected > 0;
            } else {
                String insertQuery = "INSERT INTO FVM_MONITOR_ALERTING "
                        + "(MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, BG_JOB_CRON_EXPRESSION, MB_WATCHDOG_CRON_EXPRESSION, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                int rowsAffected = jdbcTemplate.update(insertQuery,
                        monitorAlerting.getMailEmpfaenger(),
                        monitorAlerting.getMailCCEmpfaenger(),
                        monitorAlerting.getMailBetreff(),
                        monitorAlerting.getMailText(),
                        monitorAlerting.getBgCron(),
                        monitorAlerting.getMbWatchdogCron(),
                        monitorAlerting.getIsActive(),
                        monitorAlerting.getRetentionTime(),
                        monitorAlerting.getMaxParallelCheck());

                return rowsAffected > 0;
            }
        } catch (Exception e) {
            logger.error("Failed to save background job configuration", e);
        } finally {
            connectionClose(jdbcTemplate);
        }
        return false;
    }

    public boolean updateIsActive(int isActive, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET IS_ACTIVE = ?";
            jdbcTemplate.update(updateQuery, isActive);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update isActive", e);
        } finally {
            connectionClose(jdbcTemplate);
        }
        return false;
    }

    public boolean updateIsBackJobActive(int isActive, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET ISBACKJOBACTIVE = ?";
            jdbcTemplate.update(updateQuery, isActive);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update isBackJobActive", e);
        } finally {
            connectionClose(jdbcTemplate);
        }
        return false;
    }

    public void updateLastAlertTimeInDatabase(MonitorAlerting monitorAlerting, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET LAST_ALERT_TIME = ?";
            jdbcTemplate.update(updateQuery, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Error while updating last alert in DB", e);
            Notification.show("Error while updating last alert in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            connectionClose(jdbcTemplate);
        }
    }

    public void updateLastAlertCheckTimeInDatabase(MonitorAlerting monitorAlerting, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET LAST_ALERT_CHECKTIME = ?";
            jdbcTemplate.update(updateQuery, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Error while updating last alert check time in DB", e);
            Notification.show("Error while updating last alert check time in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            connectionClose(jdbcTemplate);
        }
    }

    public void deleteLastAlertTimeInDatabase(Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            String deleteQuery = "UPDATE FVM_MONITOR_ALERTING SET LAST_ALERT_TIME = NULL";
            jdbcTemplate.update(deleteQuery);
        } catch (Exception e) {
            logger.error("Error while deleting last alert in DB", e);
            Notification.show("Error while deleting last alert in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            connectionClose(jdbcTemplate);
        }
    }

    public List<fvm_monitoring> getRootMonitor(List<fvm_monitoring> listOfMonitores) {
        return listOfMonitores.stream()
                .filter(monitor -> monitor.getPid() == 0)
                .collect(Collectors.toList());
    }

    public List<fvm_monitoring> getChildMonitor(fvm_monitoring parent) {
        return CockpitView.param_Liste.stream()
                .filter(monitor -> Objects.equals(monitor.getPid(), parent.getID()))
                .collect(Collectors.toList());
    }

    public List<fvm_monitoring> getParentNodes() {
        return CockpitView.param_Liste.stream()
                .filter(monitor -> monitor.getPid() == 0)
                .collect(Collectors.toList());
    }

    public fvm_monitoring getParentByPid(Integer pid) {
        return CockpitView.param_Liste.stream()
                .filter(monitor -> monitor.getID().equals(pid))
                .findFirst()
                .orElse(null);
    }

    public boolean hasChildEntries(fvm_monitoring monitor) {
        List<fvm_monitoring> children = getChildMonitor(monitor);
        return children != null && !children.isEmpty();
    }

    public void deleteMonitor(fvm_monitoring monitor, Configuration configuration) {
        String sql = "DELETE FROM FVM_MONITORING WHERE ID = ?";

        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

        try {
            int rowsAffected = jdbcTemplate.update(sql, monitor.getID());

            if (rowsAffected > 0) {
                Notification.show("Entry deleted successfully!", 3000, Notification.Position.MIDDLE);
            } else {
                Notification.show("Error: Entry not found or could not be deleted.", 5000, Notification.Position.MIDDLE);
            }

        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            logger.error("Exception during delete", e);
        } finally {
            connectionClose(jdbcTemplate);
        }
    }
}