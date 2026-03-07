package com.example.app.utils;

import com.example.app.data.entity.Configuration;
import com.example.app.data.entity.MonitorAlerting;
import com.example.app.data.entity.ServerConfiguration;
import com.example.app.data.entity.fvm_monitoring;
import com.example.app.data.service.ConfigurationService;
import com.example.app.data.service.ServerConfigurationService;
import com.example.app.service.CockpitService;
import com.example.app.views.SftpClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class BackgroundJobExecutor implements Job {

    private CockpitService cockpitService;
    private ConfigurationService configurationService;
    private ServerConfigurationService serverConfigurationService;

    private String startType;
    private Configuration configuration;
    public static boolean stopJob = false;

    private static final Logger logger = LoggerFactory.getLogger(BackgroundJobExecutor.class);
    private static int count = 0;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        serverConfigurationService = SpringContextHolder.getBean(ServerConfigurationService.class);
        cockpitService = SpringContextHolder.getBean(CockpitService.class);
        configurationService = SpringContextHolder.getBean(ConfigurationService.class);
        transactionTemplate = SpringContextHolder.getBean(TransactionTemplate.class);

        String jobDefinitionString = context.getMergedJobDataMap().getString("configuration");

        try {
            configuration = JobDefinitionUtils.deserializeJobConfDefinition(jobDefinitionString);
            executeJob(configuration);
        } catch (JsonProcessingException e) {
            throw new JobExecutionException("Error deserializing job definition", e);
        }
    }

    private static int currentThreads = 1;
    private static final Object threadLock = new Object();
    private static final Set<Integer> globalStatus = ConcurrentHashMap.newKeySet();

    public static List<ServerConfiguration> serverConfigurationList;

    private void executeJob(Configuration configuration) {
        logger.info("Starting executeJob() of {}", configuration.getName());

        MonitorAlerting monitorAlerting = fetchEmailConfiguration(configuration);
        if (monitorAlerting == null || monitorAlerting.getBgCron() == null) {
            return;
        }

        int maxParallelChecks = (monitorAlerting.getMaxParallelCheck() > 0)
                ? monitorAlerting.getMaxParallelCheck() : 1;

        int retentionTime = (monitorAlerting.getRetentionTime() > 0)
                ? monitorAlerting.getRetentionTime() : 1;

        List<fvm_monitoring> monitorings = cockpitService.getMonitoring(configuration);
        logger.info("Count QS-Checks: {}", monitorings.size());

        ExecutorService executorService = Executors.newFixedThreadPool(maxParallelChecks);

        if (monitorings.isEmpty()) {
            logger.info("No active QS-Checks found");
            return;
        }

        for (fvm_monitoring monitoring : monitorings) {
            if ("1".equals(monitoring.getIS_ACTIVE()) && monitoring.getPid() != 0) {

                logger.info("##### Start CHECK-SQL ID={} #####", monitoring.getID());

                while (currentThreads > maxParallelChecks) {
                    logger.info("Actually Threads: {} => too many threads, sleep 2 sec.", currentThreads);
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        logger.warn("Sleep interrupted: {}", e.getMessage());
                    }
                }

                try {
                    cleanUpOldResults(monitoring.getRetentionTime(), configuration, monitoring.getID());

                    JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

                    Timestamp lastCheck = jdbcTemplate.queryForObject(
                            "SELECT MAX(Zeitpunkt) FROM FVM_MONITOR_RESULT WHERE ID = ?",
                            new Object[]{monitoring.getID()},
                            Timestamp.class
                    );

                    logger.info("Last Checktime of QS-ID {}: {}", monitoring.getID(), lastCheck);

                    long timeSinceLastCheck = (lastCheck != null)
                            ? Duration.between(lastCheck.toLocalDateTime(), LocalDateTime.now()).toMinutes()
                            : Long.MAX_VALUE;

                    synchronized (threadLock) {
                        logger.info("TimeSinceLastCheck of QS-ID {} = {} Min. CheckInterval is {} Min.",
                                monitoring.getID(), timeSinceLastCheck, monitoring.getCheck_Intervall());
                        logger.info("CurrentThreads {} Max Threads {}", currentThreads, maxParallelChecks);

                        if (timeSinceLastCheck >= monitoring.getCheck_Intervall()
                                && currentThreads <= maxParallelChecks
                                && !globalStatus.contains(monitoring.getID())) {

                            currentThreads++;
                            globalStatus.add(monitoring.getID());
                            logger.info("Requirements met, add Thread. CurrentThreads now: {}", currentThreads);

                            executorService.submit(() -> {
                                if (monitoring.getType().contains("SQL")) {
                                    logger.info("Sql monitor start execution");
                                    executeMonitoringTask(monitoring, jdbcTemplate);
                                } else if (monitoring.getType().contains("Shell")) {
                                    logger.info("Shell monitor start execution");
                                    executeShellMonitor(monitoring, jdbcTemplate);
                                }
                            });
                        }
                    }

                } catch (Exception e) {
                    logger.error("Error executing monitoring ID {} - {}", monitoring.getID(), e.getMessage(), e);
                }
            }
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error("Executor service interrupted: {}", e.getMessage(), e);
        }
    }

    private void executeMonitoringTask(fvm_monitoring monitoring, JdbcTemplate jdbcTemplate) {
        logger.info("Method executeMonitoringTask called");
        logger.info("Value of stopJob: {}", stopJob);

        try {
            if (stopJob) {
                return;
            }

            transactionTemplate.execute(status -> {
                try {
                    String sqlQuery = monitoring.getSQL();
                    String result = jdbcTemplate.queryForObject(sqlQuery, String.class);

                    logger.info("Store result for ID {}: {}", monitoring.getID(), result);

                    Integer activeRowCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM FVM_MONITOR_RESULT WHERE IS_ACTIVE = 1 AND ID = ?",
                            Integer.class,
                            monitoring.getID());

                    if (activeRowCount != null && activeRowCount > 0) {
                        jdbcTemplate.update(
                                "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID = ?",
                                monitoring.getID());
                    }

                    jdbcTemplate.update(
                            "INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                            monitoring.getID(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            1,
                            result,
                            "Query executed successfully");

                } catch (Exception ex) {
                    status.setRollbackOnly();

                    Integer activeRowCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM FVM_MONITOR_RESULT WHERE IS_ACTIVE = 1 AND ID = ?",
                            Integer.class,
                            monitoring.getID());

                    if (activeRowCount != null && activeRowCount > 0) {
                        jdbcTemplate.update(
                                "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID = ?",
                                monitoring.getID());
                    }

                    jdbcTemplate.update(
                            "INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                            monitoring.getID(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            1,
                            null,
                            ex.getMessage());

                    logger.info("{} ---- query error result store: {}", monitoring.getID(), monitoring.getSQL());
                } finally {
                    synchronized (threadLock) {
                        currentThreads--;
                        globalStatus.remove(monitoring.getID());
                    }
                }
                return null;
            });

        } catch (Exception e) {
            logger.error("Error SQL for monitoring ID {} - {}", monitoring.getID(), e.getMessage(), e);
        }
    }

    private void executeShellMonitor(fvm_monitoring monitoring, JdbcTemplate jdbcTemplate) {
        logger.info("Method executeShellMonitor called");
        logger.info("Value of stopJob: {}", stopJob);

        String shellCommand = monitoring.getShellCommand();
        String resultMsg = "ShellCommand " + shellCommand + " executed successfully";

        serverConfigurationList = serverConfigurationService.findAllConfigurations();

        try {
            if (stopJob) {
                return;
            }

            try {
                if (shellCommand != null) {
                    String server = monitoring.getShellServer();
                    logger.info("Execute script on server: {}", server);

                    if (serverConfigurationList == null) {
                        logger.error("ServerConfigurationList is null");
                        return;
                    }

                    ServerConfiguration serverConfiguration = serverConfigurationList.stream()
                            .filter(entity -> entity.getHostAlias().equals(server))
                            .findFirst()
                            .orElse(null);

                    if (serverConfiguration == null) {
                        logger.error("No server configuration found for {}", server);
                        return;
                    }

                    String username = serverConfiguration.getUserName();
                    String host = serverConfiguration.getHostName();

                    SftpClient cl = new SftpClient(
                            host,
                            Integer.parseInt(serverConfiguration.getSshPort()),
                            username
                    );

                    cl.authKey(serverConfiguration.getSshKey(), "");

                    String result = cl.executeBackgroundShellCommand(shellCommand);
                    logger.info("Shell command executed {} and result: {}", shellCommand, result);

                    if (result == null) {
                        logger.error("Result is null");
                        result = "99999";
                        resultMsg = "ShellCommand " + shellCommand + " returns null => set to 99999";
                    }

                    Integer activeRowCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM FVM_MONITOR_RESULT WHERE IS_ACTIVE = 1 AND ID = ?",
                            Integer.class,
                            monitoring.getID());

                    if (activeRowCount != null && activeRowCount > 0) {
                        jdbcTemplate.update(
                                "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID = ?",
                                monitoring.getID());
                    }

                    jdbcTemplate.update(
                            "INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                            monitoring.getID(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            1,
                            result,
                            resultMsg);

                } else {
                    logger.info("Shell command is null, not executed");
                }

            } catch (Exception ex) {
                logger.error("{} {} shell command error execution",
                        ex.getMessage(), monitoring.getID(), ex);
            } finally {
                synchronized (threadLock) {
                    currentThreads--;
                    globalStatus.remove(monitoring.getID());
                }
            }

        } catch (Exception e) {
            logger.error("Error shell monitoring ID {} - {}", monitoring.getID(), e.getMessage(), e);
        }
    }

    private void cleanUpOldResults(int retentionDays, Configuration configuration, Integer id) {
        JdbcTemplate jdbcTemplate = null;
        try {
            int rowsDeleted;
            jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

            if (id != null) {
                rowsDeleted = jdbcTemplate.update(
                        "DELETE FROM FVM_MONITOR_RESULT WHERE ID = ? AND TRUNC(Zeitpunkt) < TRUNC(SYSDATE - ?)",
                        id, retentionDays);
            } else {
                rowsDeleted = jdbcTemplate.update(
                        "DELETE FROM FVM_MONITOR_RESULT WHERE TRUNC(Zeitpunkt) < TRUNC(SYSDATE - ?)",
                        retentionDays);
            }

            logger.info("Number of rows deleted: {}", rowsDeleted);

        } catch (Exception e) {
            logger.error("Error deleting old results: {}", e.getMessage(), e);
        } finally {
            connectionClose(jdbcTemplate);
        }
    }

    public JdbcTemplate getNewJdbcTemplateWithDatabaseold(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());

        String plainPassword = configurationService.getPlainPasswordAndMigrateIfNeeded(conf);
        ds.setPassword(plainPassword);

        try {
            logger.info("{}: Connection open", conf.getUserName());
            return new JdbcTemplate(ds);
        } catch (Exception e) {
            logger.error("Error creating JdbcTemplate: {}", e.getMessage(), e);
        }
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
            logger.error("Error getting pooled JdbcTemplate: {}", e.getMessage(), e);
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
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setConnectionTimeout(30000);

        count = count + 1;

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        return new JdbcTemplate(dataSource);
    }

    public MonitorAlerting fetchEmailConfiguration(Configuration configuration) {
        MonitorAlerting monitorAlerting = new MonitorAlerting();
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

        try {
            logger.info("Executing fetchEmailConfiguration");

            String sql = "SELECT MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, WATCHDOG_MAIL_EMPFAENGER," +
                    "WATCHDOG_MAIL_CC_EMPFAENGER," +
                    "WATCHDOG_MAIL_BETREFF," +
                    "WATCHDOG_MAIL_TEXT, BG_JOB_CRON_EXPRESSION, MB_WATCHDOG_CRON_EXPRESSION," +
                    "LAST_ALERT_TIME, LAST_ALERT_CHECKTIME, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS, " +
                    "ISBACKJOBACTIVE, ISMBWATCHDOGACTIVE, SIMULATION FROM FVM_MONITOR_ALERTING";

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

        } catch (Exception e) {
            logger.error("Executing fetchEmailConfiguration from {} failed: {}",
                    configuration.getUserName(), e.getMessage(), e);
        } finally {
            connectionClose(jdbcTemplate);
        }

        return monitorAlerting;
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        // Bei gepoolten Verbindungen hier bewusst nichts schließen.
        // Das Lifecycle-Management übernimmt HikariCP.
    }
}