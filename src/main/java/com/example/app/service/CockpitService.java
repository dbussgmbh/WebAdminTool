package com.example.app.service;

import com.example.app.data.entity.Configuration;
//import com.example.app.data.entity.MonitorAlerting;
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
    private ConfigurationService configurationService;
  //  List<fvm_monitoring> listOfMonitores;
    private static final Logger logger = LoggerFactory.getLogger(CockpitService.class);

    public CockpitService(JdbcTemplate jdbcTemplate, ConfigurationService configurationService){
        this.jdbcTemplate = jdbcTemplate;
        this.configurationService = configurationService;
    }

    private void connectWithDatabase(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {
            this.jdbcTemplate = new JdbcTemplate(ds);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public JdbcTemplate getJdbcTemplateWithDBConnetion(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {
           return this.jdbcTemplate = new JdbcTemplate(ds);
        } catch (Exception e) {
           // e.getMessage();
            logger.error("Error while connect database: {}",conf.getName());
        }
        return null;
    }
    public JdbcTemplate getNewJdbcTemplateWithDatabaseold(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {
            return new JdbcTemplate(ds);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }
    public JdbcTemplate getNewJdbcTemplateWithDatabase(Configuration conf) {

        try {
            return new JdbcTemplate(configurationService.getActivePools().get(conf.getId()));
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public JdbcTemplate getNewJdbcTemplateWithDatabasenew(Configuration conf) {
        // Create HikariConfig object
        HikariConfig hikariConfig = new HikariConfig();

        // Set database connection parameters
        hikariConfig.setJdbcUrl(conf.getDb_Url());
        hikariConfig.setUsername(conf.getUserName());
        hikariConfig.setPassword(Configuration.decodePassword(conf.getPassword()));

        // Set maxLifetime to 2 minutes (120000 ms), after which connections are closed
        hikariConfig.setMaxLifetime(30000); // 2 minutes

        // Set idleTimeout to 1 minute (60000 ms) to close connections that are idle for over 1 minute
        hikariConfig.setIdleTimeout(30000); // 1 minute

        // Set the maximum pool size to control the number of open connections
        hikariConfig.setMaximumPoolSize(10); // Customize as needed
        hikariConfig.setPoolName("DB-pool");

        // Optionally set minimumIdle to keep fewer connections open during low usage periods
        hikariConfig.setMinimumIdle(1); // Maintain at least 1 connection

        // Set connection timeout for how long to wait for an available connection
        hikariConfig.setConnectionTimeout(30000);
        // Create HikariDataSource from HikariConfig
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        return new JdbcTemplate(dataSource);
    }
    public void connectionClose() {
//        Connection connection = null;
//        DataSource dataSource = null;
//        try {
//            jdbcTemplate.getDataSource().getConnection().close();
////            connection = jdbcTemplate.getDataSource().getConnection();
////            dataSource = jdbcTemplate.getDataSource();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            if (connection != null) {
//                try {
//                    connection.close();
//
//                    if (dataSource instanceof HikariDataSource) {
//                        ((HikariDataSource) dataSource).close();
//                    } else {
//                        dataSource.getConnection().close();
//                        System.out.println("DataSource closed..."+dataSource.getConnection().isClosed() +".....");
//                    }
//
//                } catch (SQLException e) {
//
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
//        Connection connection = null;
//        DataSource dataSource = null;
//        try {
//            jdbcTemplate.getDataSource().getConnection().close();
//            if (jdbcTemplate.getDataSource() instanceof HikariDataSource) {
//                ((HikariDataSource) jdbcTemplate.getDataSource()).close();
//            } else {
//                jdbcTemplate.setDataSource(null);
//            }
//            jdbcTemplate.setDataSource(null);
////            connection = jdbcTemplate.getDataSource().getConnection();
////            dataSource = jdbcTemplate.getDataSource();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            if (connection != null) {
//                try {
//                    connection.close();
//
//                    if (dataSource instanceof HikariDataSource) {
//                        ((HikariDataSource) dataSource).close();
//                    } else {
//                        dataSource.getConnection().close();
//                        System.out.println("DataSource closed..."+dataSource.getConnection().isClosed() +".....");
//                    }
//
//                } catch (SQLException e) {
//
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    public List<fvm_monitoring> getMonitoring(Configuration configuration) {
     //   List<fvm_monitoring> monitore = new ArrayList<>();

        //String sql = "SELECT ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT, ERROR_SCHWELLWERT FROM EKP.FVM_MONITORING";

//        String sql = "SELECT m.ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT" +
//                ", ERROR_SCHWELLWERT,mr.result as Aktueller_Wert, 100 / Error_schwellwert * case when mr.result>=Error_schwellwert then Error_Schwellwert else mr.result end  / 100 as Error_Prozent" +
//                ", Zeitpunkt, m.is_active, nvl(m.sql_detail,'select ''Detail-SQL nicht definiert'' from dual') as sql_detail FROM FVM_MONITORING m\n" +
//                "left outer join FVM_MONITOR_RESULT mr\n" +
//                "on m.id=mr.id\n" +
//                "and mr.is_active='1'";
      //  connectWithDatabase(configuration);
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
       // JdbcTemplate  jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        String sql = "SELECT m.ID,m.PID, m.Bereich, RETENTIONTIME, SQL, TYPE, SHELL_SERVER, SHELL_COMMAND, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT" +
                ", ERROR_SCHWELLWERT,mr.result as Aktueller_Wert, 100 / Error_schwellwert * case when mr.result>=Error_schwellwert then Error_Schwellwert else mr.result end  / 100 as Error_Prozent" +
                ", Zeitpunkt, m.is_active, m.sql_detail as sql_detail FROM FVM_MONITORING m\n" +
                "left outer join FVM_MONITOR_RESULT mr\n" +
                "on m.id=mr.id\n" +
                "and mr.is_active='1'";


      //  System.out.println("Abfrage EKP.FVM_Monitoring (CockpitView.java): ");
      //  System.out.println(sql);

        List<fvm_monitoring> fvmMonitorings = new ArrayList<>();
        try {
            fvmMonitorings = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(fvm_monitoring.class));

        //    listOfMonitores = fvmMonitorings;
        //    System.out.println("FVM_Monitoring eingelesen");

        } catch (Exception e) {
        //    Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            System.out.println("Exception: " + e.getMessage());
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }

        return fvmMonitorings;
    }
    private boolean tableExistsold(String tableName) {
        try {
            String checkTableSql = "SELECT COUNT(*) FROM ALL_TABLES WHERE table_name = ?";
            int tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);

            return tableCount > 0;
        } catch (Exception e) {
            System.out.println("Exception while checking table existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean tableExists(String tableName, String databaseType, JdbcTemplate jdbcTemplate) {
        try {
            String checkTableSql;
            // Switch based on the database type
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

            // Execute the query to check if the table exists
            int tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName.toUpperCase());

            return tableCount > 0;
        } catch (Exception e) {
            logger.error("Exception while checking table existence: " + e.getMessage());
            return false;
        }
    }

    public void createFvmMonitorAlertingTable(Configuration configuration) {

        String tableName = "FVM_MONITOR_ALERTING";
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

        if(jdbcTemplate == null){
            logger.error("jdbcTemplate konnte nicht initialisiert werden für User " + configuration.getUserName());
            return;
        }

        try {
          //  connectWithDatabase(configuration);

            String dbType = "oracle";
            if(configuration.getName().contains("SQLServer")) {
                dbType = "sqlserver";
            }
            logger.debug("DB: " + configuration.getName());

//            // Form the SQL statement to drop the table, considering case sensitivity and special characters
//            String dropTableSQL = "DROP TABLE \"" + schema + "\".\"" + tableName.toUpperCase() + "\"";
//            jdbcTemplate.execute(dropTableSQL);

            logger.info("Check ob Tabelle " + tableName + " bereits vorhanden ist...");
            if (!tableExists(tableName, dbType, jdbcTemplate)) {
                logger.info("Creating table: " + tableName);




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
                        + "RETENTION_TIME INT,"
                        + "MAX_PARALLEL_CHECKS INT, "
                        + "ISBACKJOBACTIVE INT, "
                        + "ISMBWATCHDOGACTIVE INT, "
                        + "SIMULATION INT, "
                        + "MB_WATCHDOG_CRON_EXPRESSION VARCHAR(255) "
                        + ")";

                jdbcTemplate.execute(createTableSQL);

                System.out.println("Table created successfully.");

                // Insert default row
                String insertRowSQL = "INSERT INTO FVM_MONITOR_ALERTING ("
                        + "MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, BG_JOB_CRON_EXPRESSION, "
                        + "LAST_ALERT_TIME, LAST_ALERT_CHECKTIME, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS, "
                        + "ISBACKJOBACTIVE, ISMBWATCHDOGACTIVE, MB_WATCHDOG_CRON_EXPRESSION, WATCHDOG_MAIL_EMPFAENGER, WATCHDOG_MAIL_CC_EMPFAENGER, WATCHDOG_MAIL_BETREFF, WATCHDOG_MAIL_TEXT, SIMULATION) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?,?,?)";

                jdbcTemplate.update(insertRowSQL,
                        "m.quaschny@t-online.de", "", "In der EKP sind Probleme", "In der EKP sind Probleme",
                        "0 0/1 * * * ?", null, null, 0, 5, 1, 1, 0, "0 0/2 * * * ?","m.quaschny@t-online.de", "", "watchdog for ekp", "In der EKP sind Probleme watchdog",0);
                logger.info("Default row inserted successfully.");
            } else {
                logger.debug("Table already exists: " + tableName);
            }
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            connectionClose(jdbcTemplate);
        }
    }

    public String getCurrentSchema() {
        // Use JdbcTemplate's execute method with a ConnectionCallback to get the schema
        return jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
            String schema = null;
            try {
                // Attempt to get the schema using the Connection.getSchema() method
                schema = connection.getSchema();

                // If getSchema() returns null or is not supported, use a fallback
                if (schema == null) {
                    DatabaseMetaData metaData = connection.getMetaData();
                    schema = metaData.getUserName(); // Fallback for Oracle or unsupported DBs
                }

                logger.info("Current Schema: " + schema);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return schema;
        });
    }

    public int fetchMaxParallel(Configuration configuration) {
        int maxParallel = 0;
        JdbcTemplate jdbcTemplate = getJdbcTemplateWithDBConnetion(configuration);
        try {
            logger.info("fetchMaxParallel: SELECT MAX_PARALLEL_CHECKS FROM FVM_MONITOR_ALERTING");
            String sql = "SELECT MAX_PARALLEL_CHECKS FROM FVM_MONITOR_ALERTING";

            maxParallel = jdbcTemplate.queryForObject(sql, Integer.class);

            logger.info("fetchMaxParallel= " + maxParallel);
            return maxParallel;
        } catch (Exception e) {
           // e.printStackTrace();
            logger.error("fetchMaxParallel liefert Fehler: "+e.getMessage());
        } finally {
            connectionClose(jdbcTemplate);
        }
        return 1;
    }
    public MonitorAlerting fetchEmailConfiguration(Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

        if (jdbcTemplate == null){
            logger.error("jdbcTemplate konnte nicht initialisiert werden für User " + configuration.getUserName());
            return null;
        }

        MonitorAlerting monitorAlerting = new MonitorAlerting();
        try {
            logger.info("fetechEmailConfiguration for " + configuration.getName() );
         //   connectWithDatabase(configuration);
        //    jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
            // Query to get the existing configuration
            String sql = "SELECT MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, WATCHDOG_MAIL_EMPFAENGER," +
                    "WATCHDOG_MAIL_CC_EMPFAENGER," +
                    "WATCHDOG_MAIL_BETREFF," +
                    "WATCHDOG_MAIL_TEXT, BG_JOB_CRON_EXPRESSION, MB_WATCHDOG_CRON_EXPRESSION,LAST_ALERT_TIME, LAST_ALERT_CHECKTIME, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS, ISBACKJOBACTIVE, ISMBWATCHDOGACTIVE, SIMULATION FROM FVM_MONITOR_ALERTING";

            // Use jdbcTemplate to query and map results to MonitorAlerting object
            jdbcTemplate.query(sql, rs -> {
                // Set the values in the MonitorAlerting object from the result set
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
                // Converting SQL Timestamp to LocalDateTime
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
            e.printStackTrace();
         //   Notification.show("Failed to load configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
        return null;
    }
    public boolean saveEmailConfiguration(MonitorAlerting monitorAlerting, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
             // connectWithDatabase(configuration);
         //   jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
            // Check if there is any existing data in the table
            String checkQuery = "SELECT COUNT(*) FROM FVM_MONITOR_ALERTING";
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class);

            if (count != null && count > 0) {
                // If record exists, update the configuration
                String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET " +
                        "MAIL_EMPFAENGER = ?, " +
                        "MAIL_CC_EMPFAENGER = ?, " +
                        "MAIL_BETREFF = ?, " +
                        "MAIL_TEXT = ?, " +
                        "IS_ACTIVE = ? ";

                int rowsAffected = jdbcTemplate.update(updateQuery,
                        monitorAlerting.getMailEmpfaenger(),
                        monitorAlerting.getMailCCEmpfaenger(),
                        monitorAlerting.getMailBetreff(),
                        monitorAlerting.getMailText(),
                    //    monitorAlerting.getCron(),
                        monitorAlerting.getIsActive()
//                        monitorAlerting.getMaxParallelCheck(),
//                        monitorAlerting.getRetentionTime()
                );

                if (rowsAffected > 0) {
                    return true;
                } else {
                    System.out.println("No configuration was updated.");
                   // Notification.show("No configuration was updated.", 5000, Notification.Position.MIDDLE);
                }
            } else {
                // If no record exists, insert a new configuration
                String insertQuery = "INSERT INTO FVM_MONITOR_ALERTING " +
                        "(MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, BG_JOB_CRON_EXPRESSION, MB_WATCHDOG_CRON_EXPRESSION IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?,?, ?)";

                int rowsAffected = jdbcTemplate.update(insertQuery,
                        monitorAlerting.getMailEmpfaenger(),
                        monitorAlerting.getMailCCEmpfaenger(),
                        monitorAlerting.getMailBetreff(),
                        monitorAlerting.getMailText(),
                        monitorAlerting.getBgCron(),
                        monitorAlerting.getMbWatchdogCron(),
                        monitorAlerting.getIsActive(),
                        monitorAlerting.getRetentionTime(),
                        monitorAlerting.getMaxParallelCheck()
                );

                if (rowsAffected > 0) {
                    return true;
                } else {
                    Notification.show("Failed to insert new configuration.", 5000, Notification.Position.MIDDLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Failed to save configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
        return false;
    }

    public boolean saveBackgoundJobConfiguration(MonitorAlerting monitorAlerting, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            // connectWithDatabase(configuration);
          //  jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

            // Check if there is any existing data in the table
            String checkQuery = "SELECT COUNT(*) FROM FVM_MONITOR_ALERTING";
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class);

            if (count != null && count > 0) {
                // If record exists, update the configuration
                String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET " +
                        "BG_JOB_CRON_EXPRESSION = ?, " +
                        "ISBACKJOBACTIVE = ?, " +
                        "RETENTION_TIME = ?, " +
                        "MAX_PARALLEL_CHECKS = ? ";

                int rowsAffected = jdbcTemplate.update(updateQuery,
                        monitorAlerting.getBgCron(),
                        monitorAlerting.getIsBackJobActive(),
                        monitorAlerting.getRetentionTime(),
                        monitorAlerting.getMaxParallelCheck()
                        //    monitorAlerting.getCron(),

//                        monitorAlerting.getMaxParallelCheck(),
//                        monitorAlerting.getRetentionTime()
                );

                if (rowsAffected > 0) {
                    return true;
                } else {
                    Notification.show("No configuration was updated.", 5000, Notification.Position.MIDDLE);
                }
            } else {
                // If no record exists, insert a new configuration
                String insertQuery = "INSERT INTO FVM_MONITOR_ALERTING " +
                        "(MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, BG_JOB_CRON_EXPRESSION, MB_WATCHDOG_CRON_EXPRESSION IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?,?, ?)";

                int rowsAffected = jdbcTemplate.update(insertQuery,
                        monitorAlerting.getMailEmpfaenger(),
                        monitorAlerting.getMailCCEmpfaenger(),
                        monitorAlerting.getMailBetreff(),
                        monitorAlerting.getMailText(),
                        monitorAlerting.getBgCron(),
                        monitorAlerting.getMbWatchdogCron(),
                        monitorAlerting.getIsActive(),
                        monitorAlerting.getRetentionTime(),
                        monitorAlerting.getMaxParallelCheck()
                );

                if (rowsAffected > 0) {
                    return true;
                } else {
                    Notification.show("Failed to insert new configuration.", 5000, Notification.Position.MIDDLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
         //   Notification.show("Failed to save configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
        return false;
    }


    public boolean updateIsActive(int isActive, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
         //   connectWithDatabase(configuration);
         //   jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET IS_ACTIVE = ?";

            // Update the database with the new configuration
            int rowsAffected = jdbcTemplate.update(updateQuery, isActive);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
        return false;
    }

    public boolean updateIsBackJobActive(int isActive, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
         //   connectWithDatabase(configuration);
         //   jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET isBackJobActive = ?";

            // Update the database with the new configuration
            int rowsAffected = jdbcTemplate.update(updateQuery, isActive);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
        return false;
    }

    public void updateLastAlertTimeInDatabase(MonitorAlerting monitorAlerting, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
          //  connectWithDatabase(configuration);
         //   jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET LAST_ALERT_TIME = ?";
            jdbcTemplate.update(updateQuery, LocalDateTime.now());
            System.out.println("Updated last alert time in database.");
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error while updating last alert in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
    }

    public void updateLastAlertCheckTimeInDatabase(MonitorAlerting monitorAlerting, Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
          //  connectWithDatabase(configuration);
         //   jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET LAST_ALERT_CHECKTIME = ?";
            jdbcTemplate.update(updateQuery, LocalDateTime.now());

            System.out.println("Updated last alert check time in database for ID: " + monitorAlerting.getId());
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error while updating last alert check time in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
    }

    public void deleteLastAlertTimeInDatabase(Configuration configuration) {
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            // Establish a connection to the database using the provided configuration
        //    connectWithDatabase(configuration);
        //    jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

            // SQL query to set LAST_ALERT_TIME to NULL, effectively deleting the timestamp
            String deleteQuery = "UPDATE FVM_MONITOR_ALERTING SET LAST_ALERT_TIME = NULL";

            // Execute the query to update the record for the given monitorAlerting ID
            jdbcTemplate.update(deleteQuery);

            // Log success message
            System.out.println("Deleted last alert time in the database.");
        } catch (Exception e) {
            // Log and show error message in case of an exception
            e.printStackTrace();
            Notification.show("Error while deleting last alert in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
    }

    public List<fvm_monitoring> getRootMonitor(List<fvm_monitoring> listOfMonitores) {
      //  System.out.println("-----------"+ listOfMonitores.size()+"-----------------------------");
        List<fvm_monitoring> rootProjects = listOfMonitores
                .stream()
                .filter(monitor -> monitor.getPid() == 0)
                .collect(Collectors.toList());

        // Log the names of root projects
        //    rootProjects.forEach(project -> System.out.println("Root Project: " + project.toString()));

        return rootProjects;
    }

    public List<fvm_monitoring> getChildMonitor(fvm_monitoring parent) {

        List<fvm_monitoring> childProjects = CockpitView.param_Liste
                .stream()
                .filter(monitor -> Objects.equals(monitor.getPid(), parent.getID()))
                .collect(Collectors.toList());

        // Log the names of child projects
        //    childProjects.forEach(project -> System.out.println("Child Project of " + parent.getName() + ": " + project.getName()));

        return childProjects;
    }

    public List<fvm_monitoring> getParentNodes() {
        return CockpitView.param_Liste
                .stream()
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
        // Assuming 'getChildMonitors' returns a list of child elements for the selected entry
        List<fvm_monitoring> children = getChildMonitor(monitor);
        return children != null && !children.isEmpty();
    }

    public void deleteMonitor(fvm_monitoring monitor, Configuration configuration) {

        String sql = "DELETE FROM FVM_MONITORING WHERE ID = ?";

        System.out.println("Deleting entry with ID: " + monitor.getID());

      //  connectWithDatabase(configuration);
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

        try {
            int rowsAffected = jdbcTemplate.update(sql, monitor.getID());

            if (rowsAffected > 0) {
                Notification.show("Entry deleted successfully!", 3000, Notification.Position.MIDDLE);
                System.out.println("Deleted FVM_Monitoring with ID: " + monitor.getID());
            } else {
                Notification.show("Error: Entry not found or could not be deleted.", 5000, Notification.Position.MIDDLE);
            }

        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            System.out.println("Exception during delete: " + e.getMessage());
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
    }
}
