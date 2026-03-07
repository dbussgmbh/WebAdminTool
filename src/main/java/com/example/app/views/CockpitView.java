package com.example.app.views;

import com.example.app.data.entity.*;
import com.example.app.data.entity.Configuration;
import com.example.app.data.service.ConfigurationService;
import com.example.app.data.service.ServerConfigurationService;
import com.example.app.service.CockpitService;
import com.example.app.service.EmailService;
import com.example.app.utils.*;
import com.example.app.views.list.MonitoringForm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.richtexteditor.RichTextEditorVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

import com.wontlost.ckeditor.*;
import jakarta.annotation.security.RolesAllowed;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PageTitle("eKP-Cokpit")
@Route(value = "cockpit", layout= MainLayout.class)
@RolesAllowed({"ADMIN","FVM", "COKPIT"})
@Menu(title = "eKP Cockpit", order =4, icon = "vaadin:cockpit")
@PreserveOnRefresh
public class CockpitView extends VerticalLayout{
    @Autowired
    JdbcTemplate jdbcTemplate;
    private EmailService emailService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final Logger logger = LoggerFactory.getLogger(CockpitView.class);
    myCallback callback=new myCallback() {
        @Override
        public void delete(fvm_monitoring mon) {
            logger.info("Delete in CockpitView aufgerufen für " + mon.getTitel());

        }

        @Override
        public void save(fvm_monitoring mon) {
            logger.info("Save in CockpitView aufgerufen für " + mon.getTitel());



            /*String sql = "update FVM_MONITORING \n" +
                    "set SQL='" + mon.getSQL() + "',\n" +
                    "TITEL='" + mon.getTitel() + "',\n" +
                    "Beschreibung='" + mon.getBeschreibung() + "',\n" +
                    "Handlungs_Info='" + mon.getHandlungs_INFO() + "',\n" +
                    "Check_Intervall=" + mon.getCheck_Intervall() + ",\n" +
                    "WARNING_SCHWELLWERT=" + mon.getWarning_Schwellwert() + ",\n" +
                    "ERROR_SCHWELLWERT=" + mon.getError_Schwellwert() + ",\n" +
                    "IS_ACTIVE='" + mon.getIS_ACTIVE() + "'\n" +
                    "SQL_Detail='"+ mon.getSQL_Detail() + "'\n" +
                    "where id=" + mon.getID();
*/


           // System.out.println("Update FVM_Monitoring (CockpitView.java):......................... "+mon.getPid());

//            DriverManagerDataSource ds = new DriverManagerDataSource();
//            com.example.application.data.entity.Configuration conf;
//            conf = comboBox.getValue();
//
//            ds.setUrl(conf.getDb_Url());
//            ds.setUsername(conf.getUserName());
//            ds.setPassword(Configuration.decodePassword(conf.getPassword()));

            try {
             //   jdbcTemplate.setDataSource(ds);
                jdbcTemplate = cockpitService.getNewJdbcTemplateWithDatabase(comboBox.getValue());

                if (mon.getID() == null) {
                    // If ID is null, perform INSERT
                    String insertSql = "INSERT INTO FVM_MONITORING " +
                            "(SQL, TITEL, Beschreibung, Handlungs_Info, Check_Intervall, WARNING_SCHWELLWERT, ERROR_SCHWELLWERT, IS_ACTIVE, SQL_Detail, PID, BEREICH, RETENTIONTIME, TYPE, SHELL_SERVER, SHELL_COMMAND) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?)";

                    jdbcTemplate.update(insertSql,
                            mon.getSQL(),
                            mon.getTitel(),
                            mon.getBeschreibung(),
                            mon.getHandlungs_INFO(),
                            mon.getCheck_Intervall(),
                            mon.getWarning_Schwellwert(),
                            mon.getError_Schwellwert(),
                            mon.getIS_ACTIVE(),
                            mon.getSQL_Detail(),
                            mon.getPid(),
                            mon.getBereich(),
                            mon.getRetentionTime(),
                            mon.getType(),
                            mon.getShellServer(),
                            mon.getShellCommand()
                    );
                    logger.info("Insert durchgeführt");

                } else {

                    String sql = "update FVM_MONITORING set " +
                            " SQL=?, " +
                            " TITEL=?," +
                            " Beschreibung=?," +
                            " Handlungs_Info=?," +
                            " Check_Intervall=?, " +
                            " WARNING_SCHWELLWERT=?, " +
                            " ERROR_SCHWELLWERT=?, " +
                            " IS_ACTIVE=?, " +
                            " SQL_Detail=?, " +
                            " PID=?, " +
                            " BEREICH=?, " +
                            " RETENTIONTIME=?," +
                            " TYPE=?," +
                            " SHELL_SERVER=?," +
                            " SHELL_COMMAND=?" +
                            " where id= ?";

                    System.out.println(sql);

                    jdbcTemplate.update(sql, mon.getSQL()
                            , mon.getTitel()
                            , mon.getBeschreibung()
                            , mon.getHandlungs_INFO()
                            , mon.getCheck_Intervall()
                            , mon.getWarning_Schwellwert()
                            , mon.getError_Schwellwert()
                            , mon.getIS_ACTIVE()
                            , mon.getSQL_Detail()
                            , mon.getPid()
                            , mon.getBereich()
                            , mon.getRetentionTime()
                            , mon.getType()
                            ,mon.getShellServer()
                            ,mon.getShellCommand()
                            , mon.getID()
                    );

                    //     jdbcTemplate.update(sql);
                    logger.info("Update durchgeführt");
                   // System.out.println("Update durchgeführt");
                }
            } catch (Exception e) {

                logger.error("Exception: " + e.getMessage());
            }
            finally {
                form.setVisible(false);
                cockpitService.connectionClose(jdbcTemplate);
            }

        }

        @Override
        public void cancel() {
            form.setVisible(false);
        }
    };
    Grid<fvm_monitoring> grid = new Grid<>(fvm_monitoring.class, false);
    private TreeGrid<fvm_monitoring> treeGrid;

    Grid<LinkedHashMap<String, Object>> grid_metadata = new Grid<>();

    Dialog dialog_Beschreibung = new Dialog();
    Dialog dialog_Editor = new Dialog();

    // RichTextEditor editor = new RichTextEditor();

    private static Map<Integer, fvm_monitoring> expandedNodesMap = new HashMap<>();

    private ComboBox<Configuration> comboBox;
    public static List<fvm_monitoring> param_Liste = new ArrayList<fvm_monitoring>();
    static List<fvm_monitoring> monitore;

    static VerticalLayout content = new VerticalLayout();;
    static Tab details;
    static Tab payment;
    //static Tab data;
    //static TextField titel;

    static fvm_monitoring akt_mon;
    private ScheduledExecutorService executor;

    Checkbox autorefresh = new Checkbox();

    private Div lastRefreshLabel;
    private Div countdownLabel;

    private ContextMenu alert_menu;
    private ContextMenu check_menu;
    private Span syscheck;
    private Span alerting;
    Button menuButton = new Button("Show/Hide Columns");

    private UI ui ;
    Instant startTime;
    ConfigurationService service;
    private CockpitService cockpitService;
    MonitoringForm form;
    private String alertingState;
   // private String emailAlertingAutostart;
    public static List<ServerConfiguration> serverConfigurationList;
    private LocalDateTime lastAlertTime = LocalDateTime.of(1970, 1, 1, 0, 0); // Initialize to epoch start



    public CockpitView(JdbcTemplate jdbcTemplate, ConfigurationService service, EmailService emailService, CockpitService cockpitService, ServerConfigurationService serverConfigurationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.service = service;
        this.emailService = emailService;
        this.cockpitService = cockpitService;

        logger.info("Starting CockpitView");
        addClassName("cockpit-view");
        setSizeFull();

      //  configureGrid();
        configureTreeGrid();

        countdownLabel = new Div("");
        lastRefreshLabel=new Div("");

        ui= UI.getCurrent();



        Button xmlBt = new Button("XML");

        xmlBt.addClickListener(e -> {
            System.out.println("XML-Button gedrückt");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://jsonplaceholder.typicode.com/albums")).build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    //.thenAccept(System.out::println)s
                    //.thenApply(CockpitView::parse)
                    .thenApply(CockpitView::xmlpare)
                    .join();
        });

        HorizontalLayout hl = new HorizontalLayout();
        H2 h2 = new H2("ekP / EGVP-E Monitoring");
        hl.add(h2,menuButton);
        add(hl);

        //editor.setVisible(false);

        // String htmlContent = "<h1>Beispielüberschrift</h1><p>Dies ist der Inhalt des Editors.</p>";
        // editor.asHtml().setValue(htmlContent);

        //add(editor);
        //editor.setVisible(false);



        Button closeButton = new Button("close", e -> dialog_Beschreibung.close());
        Button cancelEditButton = new Button("cancel", e -> dialog_Editor.close());
        Button saveEditButton = new Button("save", e -> {

            saveMonitor();
            dialog_Editor.close();
        });
        dialog_Beschreibung.getFooter().add(closeButton);

        dialog_Editor.getFooter().add(cancelEditButton,saveEditButton);


        //   HorizontalLayout layout = new HorizontalLayout(comboBox,refreshBtn);



        form = new MonitoringForm(callback);
        form.setVisible(false);

        serverConfigurationList = serverConfigurationService.findAllConfigurations();

        //add(getToolbar(),grid,form );
        add(getToolbar(),treeGrid,form );

    }

    public JdbcTemplate getJdbcTemplateWithDBConnetion(com.example.app.data.entity.Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(com.example.app.data.entity.Configuration.decodePassword(conf.getPassword()));
        try {
            jdbcTemplate.setDataSource(ds);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    private void setAlerting(String status) {
        // Update checked state of menu items
        alert_menu.getItems().forEach(item -> item.setChecked(item.getText().equals(status)));

        alerting.setText(status);
        alertingState = status;
        logger.info("setAlerting:" +status);

    }


    private void setChecker(String status) {
        // Update checked state of menu items
        check_menu.getItems().forEach(item -> item.setChecked(item.getText().equals(status)));
        syscheck.setText(status);
        logger.info("setChecker:" +status);


    }

    public void scheduleEmailMonitorJob(Configuration configuration) throws SchedulerException {
        logger.info("Starting scheduleEmailMonitorJob");
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        try {
            jobDataMap.put("configuration", JobDefinitionUtils.serializeJobDefinition(configuration));
        } catch (JsonProcessingException e) {
            Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            return;
        }

        jobDataMap.put("startType", "cron");

        JobDetail jobDetail = JobBuilder.newJob(EmailMonitorJobExecutor.class)
                .withIdentity("job-alert-cron-" + configuration.getId(), "Email_group")
                .usingJobData(jobDataMap)
                .build();

        // Fetch monitorAlerting configuration to get the interval
        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(configuration);
   //     System.out.println("---------------------------------------"+monitorAlerting.getMailEmpfaenger()+"--------------------------------------");
        if (monitorAlerting == null || monitorAlerting.getBgCron() == null) {
            //System.out.println("No interval set for the configuration. Job will not be scheduled.");
            logger.error("No interval set for the configuration. Job will not be scheduled.");
            return;
        }

        String cronExpression = monitorAlerting.getBgCron();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-alert-cron-" + configuration.getId(), "Email_group")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void scheduleBackgroundJob(Configuration configuration) throws SchedulerException {
        logger.info("Starting scheduleBackgroundJob");
        BackgroundJobExecutor.stopJob = false;
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        try {
            jobDataMap.put("configuration", JobDefinitionUtils.serializeJobDefinition(configuration));
        } catch (JsonProcessingException e) {
            Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            return;
        }

        jobDataMap.put("startType", "cron");

        JobDetail jobDetail = JobBuilder.newJob(BackgroundJobExecutor.class)
                .withIdentity("job-background-cron-" + configuration.getId(), "Check_Group")
                .usingJobData(jobDataMap)
                .build();

        // Fetch monitorAlerting configuration to get the interval
        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(configuration);

        if (monitorAlerting == null || monitorAlerting.getBgCron() == null) {
            System.out.println("No interval set for the configuration. Job will not be scheduled.");
            return;
        }

        String cronExpression = monitorAlerting.getBgCron();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-background -cron-" + configuration.getId(), "Check_Group")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    private String createCronExpression(int interval) {
        // Cron expression format for every N minutes
        return "0 0/" + interval + " * * * ?";
    }
    private void stopAllScheduledJobs(Configuration configuration) {
        logger.info("Executing stopAllScheduledJobs");
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            JobKey cronJobKey = new JobKey("job-alert-cron-" + configuration.getId(), "Email_group");

            // Try stopping cron job
            if (scheduler.checkExists(cronJobKey)) {
                if (scheduler.deleteJob(cronJobKey)) {
                    System.out.println("stop alert job successful "+ configuration.getName());
                    Notification.show("Cron job " + configuration.getName() + " stopped successfully,," + configuration.getId());
                }
            }


        } catch (Exception e) {
            logger.error("Executing stopAllScheduledJobs: Error stopping jobs:");
            Notification.show("Error stopping jobs: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void stopBackgroundScheduledJobs(Configuration configuration) {
        logger.info("Executing stopBackgroundScheduledJobs");
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            JobKey cronJobKey = new JobKey("job-background-cron-" + configuration.getId(), "Check_Group");

            // Try stopping cron job
            if (scheduler.checkExists(cronJobKey)) {
                if (scheduler.deleteJob(cronJobKey)) {
                    BackgroundJobExecutor.stopJob = true;
                    System.out.println("stop bachground job successful "+ configuration.getName());
                    Notification.show("Cron job " + configuration.getName() + " stopped successfully,," + configuration.getId());
                }
            }


        } catch (Exception e) {
            logger.error("Executing stopBackgroundScheduledJobs: Error stopping jobs:");
            Notification.show("Error stopping jobs: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
    private void startMonitoringJob() {

        // Start or resume the Quartz job
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            System.out.println("Monitoring job started.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopMonitoringJob() {
        // Pause or stop the Quartz job
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            //   scheduler.pauseJob(new JobKey("monitoringJob"));
            System.out.println("Monitoring job paused.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void saveMonitor() {
        System.out.println("Titel:" + akt_mon.getTitel());

        System.out.println("Save gedrückt");
    }

    private Component getToolbar() {
        logger.info("Executing getToolbar");
        Button refreshBtn = new Button("refresh");
        refreshBtn.getElement().setProperty("title", "Daten neu einlesen");
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        refreshBtn.addClickListener(clickEvent -> {

       //     updateGrid();
            updateTreeGrid();
            updateLastRefreshLabel();

        });

        comboBox = new ComboBox<>("Verbindung");
        try {
            List<Configuration> configList = service.findMessageConfigurations();
            if (configList != null && !configList.isEmpty()) {

                List<Configuration> filteredConfigList = configList.stream()
                        .filter(config -> config.getIsMonitoring() != null && config.getIsMonitoring() == 1)
                        .toList();
                if (!filteredConfigList.isEmpty()) {
                    comboBox.setItems(filteredConfigList);
                    comboBox.setItemLabelGenerator(Configuration::getName);
                    comboBox.setValue(filteredConfigList.get(0)); // Set the first item as the default value
                } else {
                    Notification.show("No configurations available for monitoring.", 5000, Notification.Position.MIDDLE);
                }
            }
            updateTreeGrid();
        //    updateGrid();
        } catch (Exception e) {
            // Display the error message to the user
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }

        comboBox.addValueChangeListener(event -> {

            if (comboBox.getValue() == null)
            {
                logger.info("Keine DB ausgewählt");
                return;
            }

            //save value to local web storage
            WebStorage.setItem(WebStorage.Storage.SESSION_STORAGE,"CockpitVerbindungId", ""+comboBox.getValue().getId());

            getJdbcTemplateWithDBConnetion(comboBox.getValue());

            updateTreeGrid();
        //    updateGrid();
            cockpitService.createFvmMonitorAlertingTable(comboBox.getValue());
            MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(event.getValue());
            if(monitorAlerting != null && monitorAlerting.getIsActive() == 1) {
                setAlerting("On");
            } else {
                setAlerting("Off");
            }
            if(monitorAlerting != null && monitorAlerting.getIsBackJobActive() == 1) {
                setChecker("On");
            } else {
                setChecker("Off");
            }
        });

        //get last choice
        WebStorage.getItem(WebStorage.Storage.SESSION_STORAGE, "CockpitVerbindungId", value -> {
            logger.debug("Cockpit Last Verbindung ID: " + value);
            if(value != null) {
                Configuration configuration = service.findByIdConfiguration(Long.valueOf(value));
                logger.debug("Cockpit Last Verbindung name: " + configuration.getName());
                comboBox.setValue(configuration);
            }
        });

        autorefresh.setLabel("Autorefresh");

        autorefresh.addDoubleClickListener(e -> {

            logger.debug("Doppelklick auf Autorefresh Label!");

        });

        autorefresh.addClickListener(e -> {

            if (autorefresh.getValue()) {
                logger.debug("Autorefresh wird eingeschaltet.");
                startCountdown(Duration.ofSeconds(60));
                countdownLabel.setVisible(true);
            } else {
                logger.debug("Autorefresh wird ausgeschaltet.");
                stopCountdown();
                countdownLabel.setVisible(false);
            }

        });


        updateLastRefreshLabel();

        syscheck = new Span();
        check_menu = new ContextMenu();
        check_menu.setTarget(syscheck);

        alerting = new Span();
        //  add(xmlBt);
        alert_menu = new ContextMenu();
        alert_menu.setTarget(alerting);

// Add "On" menu item and mark it checkable
        MenuItem onMenuItem = alert_menu.addItem("On", event -> {
            setAlerting("On");
            Configuration configuration = comboBox.getValue();
            cockpitService.updateIsActive(1, configuration);
            cockpitService.deleteLastAlertTimeInDatabase(configuration);
            checkForAlert();
            System.out.println("Alerting Mail eingeschaltet");
        });
        onMenuItem.setCheckable(true); // Ensure the "On" item is checkable

// Add "Off" menu item and mark it checkable
        MenuItem offMenuItem = alert_menu.addItem("Off", event -> {
            setAlerting("Off");
            cockpitService.updateIsActive(0, comboBox.getValue());
            System.out.println("Alerting eMail ausgeschaltet");
            checkForAlert();
        });
        offMenuItem.setCheckable(true); // Ensure the "Off" item is checkable

        // Add "E-Mail Konfiguration" menu item without checkable option (no need)
        alert_menu.addItem("E-Mail Konfiguration", event -> {
            System.out.println("Konfig-Dialog aufrufen");
            emailConfigurationDialog();
        });

        MonitorAlerting  monitorAlerting=new MonitorAlerting();

        if (comboBox.getValue() == null)
        {
           logger.info("Keine DB ausgewählt");
        }
        else {
            cockpitService.createFvmMonitorAlertingTable(comboBox.getValue());
            monitorAlerting = cockpitService.fetchEmailConfiguration(comboBox.getValue());
        }


        if (monitorAlerting != null && monitorAlerting.getIsActive() != null && monitorAlerting.getIsActive() != 0) {
            setAlerting("On");
        } else {
            setAlerting("Off");
        }

        Div alertingInfo = new Div(new Span("eMail-Alerting: "), alerting);
        alerting.getStyle().set("font-weight", "bold");


        // Add "On" menu item and mark it checkable
        MenuItem onMenuItemChecker = check_menu.addItem("On", event -> {

            System.out.println("Background Job for checker eingeschaltet");
            setChecker("On");
            cockpitService.updateIsBackJobActive(1, comboBox.getValue());
            checkBackgroundProcess();
        });
        onMenuItemChecker.setCheckable(true); // Ensure the "On" item is checkable

        // Add "Off" menu item and mark it checkable
        MenuItem offMenuItemChecker = check_menu.addItem("Off", event -> {
            System.out.println("Background Job for checker ausgeschaltet");
            setChecker("Off");
            cockpitService.updateIsBackJobActive(0, comboBox.getValue());
            checkBackgroundProcess();
        });
        offMenuItemChecker.setCheckable(true);

        // Add "Cron Expression" menu item
        check_menu.addItem("Konfiguration", event -> {
            System.out.println("Call Dialog for cron expression");
            backGroundConfigurationDialog();
        });

        if (monitorAlerting != null && monitorAlerting.getIsBackJobActive() != null && monitorAlerting.getIsBackJobActive() != 0) {
            setChecker("On");
        } else {
            setChecker("Off");
        }

        Div checkInfo = new Div(new Span("Background-Job: "), syscheck);
        syscheck.getStyle().set("font-weight", "bold");

       // Div alertingInfo = new Div(new Span("eMail-Alerting: "), alerting);

        HorizontalLayout layout = new HorizontalLayout(comboBox,refreshBtn, lastRefreshLabel, autorefresh, countdownLabel, checkInfo, alertingInfo);
        layout.setPadding(false);
        layout.setAlignItems(Alignment.BASELINE);

        return layout;

    }

    private void updateLastRefreshLabel() {
        LocalTime currentTime = LocalTime.now();
        String formattedTime = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logger.info("updateLastRefreshLabel: letzte Aktualisierung: " + formattedTime);
        lastRefreshLabel.setText("letzte Aktualisierung: " + formattedTime);
    }

//    private void updateGrid() {
//        param_Liste = cockpitService.getMonitoring(comboBox.getValue());
//        if(param_Liste != null) {
//            grid.setItems(param_Liste);
//        }
//    }

    private void updateTreeGrid() {
        param_Liste = cockpitService.getMonitoring(comboBox.getValue());
      //  System.out.println(param_Liste.size()+"............fffffffffffffffffffffffffff");
        if(param_Liste != null) {
            List<fvm_monitoring> rootItems = cockpitService.getRootMonitor(param_Liste);
            treeGrid.setItems(rootItems, cockpitService ::getChildMonitor);
            logger.info("updateTreeGrid");

            logger.debug("Try to expand tree...");
       //     logger.info("Expanded-Nodes: " + expanded_nodes.size());
            //treeGrid.expandRecursively(expanded_nodes,2);
            //treeGrid.expand(expanded_nodes);
            //treeGrid.scrollToIndex(1);
            //treeGrid.scrollToEnd();


            List<fvm_monitoring> expandedItems = rootItems.stream()
                    .filter(rootItem -> expandedNodesMap.containsKey(rootItem.getID()))
                    .collect(Collectors.toList());

            logger.info("Expanded-Nodes: " + expandedNodesMap.size());

            treeGrid.expand(expandedItems);

        }

    }

    private void configureTreeGrid() {
        treeGrid = new TreeGrid<>();
        logger.info("configureTreeGrid");
        // Add the hierarchy column for displaying the hierarchical data
        treeGrid.addHierarchyColumn(fvm_monitoring::getBereich).setHeader("Bereich").setWidth("80px").setResizable(true);

        Grid.Column<fvm_monitoring> idColumn = treeGrid.addColumn((fvm_monitoring::getID)).setHeader("ID")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<fvm_monitoring> pidColumn = treeGrid.addColumn((fvm_monitoring::getPid)).setHeader("PID")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        treeGrid.addColumn(fvm_monitoring::getTitel).setHeader("Titel")
                .setWidth("350px").setResizable(true).setSortable(true);
      /*  grid.addColumn(fvm_monitoring::getCheck_Intervall).setHeader("Intervall")
                .setAutoWidth(true).setResizable(true).setSortable(true); */
//        grid.addColumn(fvm_monitoring::getWarning_Schwellwert).setHeader("Warning Schwellwert")
//                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<fvm_monitoring> warnSchwellwerkColumn = treeGrid.addColumn((fvm_monitoring::getWarning_Schwellwert)).setHeader("Warning Schwellwert")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<fvm_monitoring> retentionColumn = treeGrid.addColumn((fvm_monitoring::getRetentionTime)).setHeader("Retention Time")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        treeGrid.addColumn(fvm_monitoring::getError_Schwellwert ).setHeader("Error Schwellwert")
                .setWidth("30px").setResizable(true).setSortable(true);
//        treeGrid.addColumn(fvm_monitoring::getAktueller_Wert).setHeader("Aktuell")
//                .setWidth("30px").setResizable(true).setSortable(true);
        treeGrid.addColumn(item -> {
                    if (item.getPid() == 0) {
                        return "";
                    }
                    return item.getAktueller_Wert();
                }).setHeader("Aktuell")
                .setWidth("30px").setResizable(true).setSortable(true);
        //  grid.addColumn(fvm_monitoring::getBeschreibung).setHeader("Beschreibung")
        //          .setAutoWidth(true).setResizable(true).setSortable(true);
        //  grid.addColumn(fvm_monitoring::getHandlungs_INFO).setHeader("Handlungsinfo")
        //          .setAutoWidth(true).setResizable(true).setSortable(true);

        // Spalte für den Fortschritt mit ProgressBarRenderer
        treeGrid.addColumn(new ComponentRenderer<>(item -> {
            if (item.getPid() == 0) {
                return new Text("");
            }
            ProgressBar progressBar = new ProgressBar();

            progressBar.setValue(item.getError_Prozent()); // Wert zwischen 0 und 1
            //progressBar.setValue(0.8); // Wert zwischen 0 und 1

            Double p = item.getError_Prozent();
            Double rounded;
            p = p*100;
            rounded = (double) Math.round(p);
            Text t = new Text(rounded.toString() + "%");
            HorizontalLayout hl = new HorizontalLayout();
            hl.add(t,progressBar);

            return hl;
        })).setHeader("Auslastung").setWidth("100px").setResizable(true);

//        treeGrid.addColumn(fvm_monitoring::getIS_ACTIVE).setHeader("Aktiv")
//                .setWidth("30px").setResizable(true).setSortable(true);

        treeGrid.addColumn(item -> {
                    if (item.getPid() == 0) {
                        return "";
                    }
                    return item.getIS_ACTIVE();
                }).setHeader("Aktiv")
                .setWidth("30px").setResizable(true).setSortable(true);

        treeGrid.setItemDetailsRenderer(createPersonDetailsRenderer());
        treeGrid.setSelectionMode(Grid.SelectionMode.NONE);
        // grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        // grid.setDetailsVisibleOnClick(false);

        treeGrid.addItemClickListener(event -> {

            //  System.out.println("ClickEvent:" + event.getItem().getTitel());

            if (((ClickEvent) event).getClickCount() == 2){
                System.out.println("Double ClickEvent:" + event.getItem().getTitel());
                treeGrid.setDetailsVisible(event.getItem(), !treeGrid.isDetailsVisible(event.getItem()));
            }

            //   ClickEvent<fvm_monitoring> clickEvent = (ClickEvent<fvm_monitoring>) event;
            //   if (clickEvent.getClickCount() == 2) {
            //       grid.setDetailsVisible(event.getItem(), !grid.isDetailsVisible(event.getItem()));
            //   }
        });


//        if(param_Liste != null) {
//            treeGrid.setItems(param_Liste);
//            listOfJobManager = jobDefinitionService.findAll();
//            List<JobManager> rootItems = jobDefinitionService.getRootJobManager();
//            treeGrid.setItems(rootItems, jobDefinitionService ::getChildJobManager);
//        }
        treeGrid.setHeight("800px");
        treeGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        treeGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        treeGrid.setThemeName("dense");

        MonitorContextMenu contextMenu = new MonitorContextMenu(treeGrid);

        treeGrid.setPartNameGenerator(person -> {

            if (person.getAktueller_Wert() != null
                    && person.getWarning_Schwellwert() != null
                    && person.getError_Schwellwert() != null) {

                if (person.getAktueller_Wert() >= person.getWarning_Schwellwert()
                        && person.getAktueller_Wert() < person.getError_Schwellwert()) {
                    return "warning";
                }

                if (person.getAktueller_Wert() >= person.getError_Schwellwert()) {
                    return "error";
                }
            }

            // Fehlerstatus von Kindern nach oben propagieren
            if (hasChildWithError(person)) {
                return "error";
            }

            return null;
        });


        treeGrid.addExpandListener(event -> {
            // System.out.println("yes..."+event.getItems().size());
            for (fvm_monitoring item : event.getItems()) {
                expandedNodesMap.put(item.getID(), item);
            }
            logger.info("Tree expanded..."+ event.getItems().size()+" expanded node: "+expandedNodesMap.size());
        });

        treeGrid.addCollapseListener(event -> {
            // System.out.println("yes..."+event.getItems().size());
            for (fvm_monitoring item : event.getItems()) {
                expandedNodesMap.remove(item.getID());
            }
            logger.info("Tree collapsed..."+ event.getItems().size()+" expanded node: "+expandedNodesMap.size());
        });

        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        idColumn.setVisible(false);
        pidColumn.setVisible(false);
        warnSchwellwerkColumn.setVisible(false);
        retentionColumn.setVisible(false);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(menuButton);
        columnToggleContextMenu.addColumnToggleItem("ID", idColumn);
        columnToggleContextMenu.addColumnToggleItem("PID", pidColumn);
        columnToggleContextMenu.addColumnToggleItem("Warning Schwellwert", warnSchwellwerkColumn);
        columnToggleContextMenu.addColumnToggleItem("Retention Time", retentionColumn);


    }

    private boolean hasChildWithError(fvm_monitoring person) {
        // Retrieve children for the given person (use your service logic for getting children)
        List<fvm_monitoring> children = cockpitService.getChildMonitor(person);

        for (fvm_monitoring child : children) {
            // Check if the child itself has an error
            if (child.getAktueller_Wert() != null && child.getError_Schwellwert() != null &&
                    child.getAktueller_Wert() >= child.getError_Schwellwert()) {
                return true;
            }

            // Recursively check if any of the child's children have an error
            if (hasChildWithError(child)) {
                return true;
            }
        }

        // No child with error found
        return false;
    }
    private void configureGrid() {
        /*grid.addColumn(fvm_monitoring::getID).setHeader("ID")
                .setAutoWidth(true).setResizable(true).setSortable(true);*/
        Grid.Column<fvm_monitoring> idColumn = grid.addColumn((fvm_monitoring::getID)).setHeader("ID")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<fvm_monitoring> pidColumn = grid.addColumn((fvm_monitoring::getPid)).setHeader("PID")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        grid.addColumn(fvm_monitoring::getTitel).setHeader("Titel")
                .setAutoWidth(true).setResizable(true).setSortable(true);
      /*  grid.addColumn(fvm_monitoring::getCheck_Intervall).setHeader("Intervall")
                .setAutoWidth(true).setResizable(true).setSortable(true); */
//        grid.addColumn(fvm_monitoring::getWarning_Schwellwert).setHeader("Warning Schwellwert")
//                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<fvm_monitoring> warnSchwellwerkColumn = grid.addColumn((fvm_monitoring::getWarning_Schwellwert)).setHeader("Warning Schwellwert")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        grid.addColumn(fvm_monitoring::getError_Schwellwert ).setHeader("Error Schwellwert")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        grid.addColumn(fvm_monitoring::getAktueller_Wert).setHeader("Aktuell")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        //  grid.addColumn(fvm_monitoring::getBeschreibung).setHeader("Beschreibung")
        //          .setAutoWidth(true).setResizable(true).setSortable(true);
        //  grid.addColumn(fvm_monitoring::getHandlungs_INFO).setHeader("Handlungsinfo")
        //          .setAutoWidth(true).setResizable(true).setSortable(true);

        // Spalte für den Fortschritt mit ProgressBarRenderer
        grid.addColumn(new ComponentRenderer<>(item -> {
            ProgressBar progressBar = new ProgressBar();

            progressBar.setValue(item.getError_Prozent()); // Wert zwischen 0 und 1
            //progressBar.setValue(0.8); // Wert zwischen 0 und 1

            Double p = item.getError_Prozent();
            Double rounded;
            p = p*100;
            rounded = (double) Math.round(p);
            Text t = new Text(rounded.toString() + "%");
            HorizontalLayout hl = new HorizontalLayout();
            hl.add(t,progressBar);

            return hl;
        })).setHeader("Auslastung").setWidth("150px").setResizable(true);

        grid.addColumn(fvm_monitoring::getIS_ACTIVE).setHeader("Aktiv")
                .setAutoWidth(true).setResizable(true).setSortable(true);


        grid.setItemDetailsRenderer(createPersonDetailsRenderer());
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        // grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        // grid.setDetailsVisibleOnClick(false);

        grid.addItemClickListener(event -> {

            //  System.out.println("ClickEvent:" + event.getItem().getTitel());

            if (((ClickEvent) event).getClickCount() == 2){
                System.out.println("Double ClickEvent:" + event.getItem().getTitel());
                grid.setDetailsVisible(event.getItem(), !grid.isDetailsVisible(event.getItem()));
            }

            //   ClickEvent<fvm_monitoring> clickEvent = (ClickEvent<fvm_monitoring>) event;
            //   if (clickEvent.getClickCount() == 2) {
            //       grid.setDetailsVisible(event.getItem(), !grid.isDetailsVisible(event.getItem()));
            //   }
        });


        if(param_Liste != null) {
            grid.setItems(param_Liste);
        }
        grid.setHeight("800px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setThemeName("dense");

        MonitorContextMenu contextMenu = new MonitorContextMenu(grid);

        grid.setPartNameGenerator(person -> {

            if (person.getAktueller_Wert() != null
                    && person.getWarning_Schwellwert() != null
                    && person.getError_Schwellwert() != null) {

                if (person.getAktueller_Wert() >= person.getWarning_Schwellwert()
                        && person.getAktueller_Wert() < person.getError_Schwellwert()) {
                    return "warning";
                }

                if (person.getAktueller_Wert() >= person.getError_Schwellwert()) {
                    return "error";
                }
            }

            return null;
        });



/*
        grid.addComponentColumn(file -> {
            MenuBar menuBar = new MenuBar();
            menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
            MenuItem menuItem = menuBar.addItem("•••");
            menuItem.getElement().setAttribute("aria-label", "More options");
            SubMenu subMenu = menuItem.getSubMenu();
            subMenu.addItem("Beschreibung", event -> {



                fvm_monitoring currentItem = new fvm_monitoring();
                try {
                    currentItem = grid.getSelectionModel().getFirstSelectedItem().get();
                }
                catch ( Exception e)
                {};

                if(currentItem != null){

                System.out.println("Beschreibung ausgewählt für ID: " + currentItem.getID() );
                dialog_Beschreibung.setHeaderTitle("Beschreibung des Monitors >" + currentItem.getTitel() +"<");

                VerticalLayout dialogLayout = createDialogLayout(currentItem.getBeschreibung());
                dialog_Beschreibung.removeAll();
                dialog_Beschreibung.add(dialogLayout);
                dialog_Beschreibung.setModal(false);
                dialog_Beschreibung.setDraggable(true);
                dialog_Beschreibung.setResizable(true);
                dialog_Beschreibung.open();
                }
            });
            subMenu.addItem("Handlungsanweisung", event -> {
                System.out.println("Handlungsanweisung ausgewählt...");
            });
            return menuBar;
        }).setWidth("120px").setFlexGrow(0);*/

        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        idColumn.setVisible(false);
        warnSchwellwerkColumn.setVisible(false);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(menuButton);
        columnToggleContextMenu.addColumnToggleItem("ID", idColumn);
        columnToggleContextMenu.addColumnToggleItem("PID", idColumn);
        columnToggleContextMenu.addColumnToggleItem("Warning Schwellwert", warnSchwellwerkColumn);
    }

    public void checkForAlert() {
        Configuration configuration = comboBox.getValue();
        // Only proceed if alerting is set to "On"
        if ("On".equals(alertingState)) {
            try {
                Notification.show("Starting Alert job executing.... " + configuration.getName(), 5000, Notification.Position.MIDDLE);
                logger.info("checkForAlert: Starting Alert job executing");
                scheduleEmailMonitorJob(configuration);
            } catch (Exception e) {
                Notification.show("Error executing job: " + configuration.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        } else {
            // If alerting is "Off", stop all scheduled jobs
            stopAllScheduledJobs(configuration);
        }
    }

    public void checkBackgroundProcess() {
        Configuration configuration = comboBox.getValue();
        // Only proceed if alerting is set to "On"
        if ("On".equals(syscheck.getText())) {
            try {
                Notification.show("Starting background job executing.... " + configuration.getName(), 5000, Notification.Position.MIDDLE);
                BackgroundJobExecutor.stopJob = false;
                logger.info("checkBackgroundProcess: Starting background job executing");
                scheduleBackgroundJob(configuration);
            } catch (Exception e) {
                Notification.show("Error executing job: " + configuration.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        } else {
            // If alerting is "Off", stop all scheduled jobs
            stopBackgroundScheduledJobs(configuration);
        }
    }
    @Scheduled(fixedRateString = "#{fetchEmailConfiguration().getIntervall() * 1000}") // Schedule based on user-defined interval
    public void checkForAlertsold() {
        if (!"On".equals(alertingState)) {
            return; // Exit if alerting is not "On"
        }

        // Fetch email configurations
        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(comboBox.getValue());

        if (monitorAlerting == null || monitorAlerting.getBgCron() == null) {
            logger.error("E-Mail Alerting not configured!");
            return; // Exit if no configuration or interval is set
        }

        // Update the LAST_ALERT_CHECKTIME column with the current datetime
        updateLastAlertCheckTimeInDatabase(monitorAlerting);

        // Check the last alert time to ensure 60 minutes have passed
        LocalDateTime lastAlertTimeFromDB = cockpitService.fetchEmailConfiguration(comboBox.getValue()).getLastAlertTime();
        if (lastAlertTimeFromDB != null && lastAlertTimeFromDB.plusMinutes(60).isAfter(LocalDateTime.now())) {
            logger.info("60 minutes have not passed since the last alert. Skipping alert.");
            return;
        }

        // Check all monitoring entries
        List<fvm_monitoring> monitorings = param_Liste;

        for (fvm_monitoring monitoring : monitorings) {

            if (!monitoring.getIS_ACTIVE().equals("1")) {
                logger.info("Skip check entry " + monitoring.getTitel() + " active= " + monitoring.getIS_ACTIVE());

                continue; // Skip non-active entries
            }
            logger.info("ShouldSendAlert(monitoring) for ID " +monitoring.getID() + ": " + shouldSendAlert(monitoring));
            if (shouldSendAlert(monitoring)) {
           //     if (shouldSendEmail(monitorAlerting)) {
                    sendAlertEmail(monitorAlerting, monitoring);
                    lastAlertTime = LocalDateTime.now(); // Update last alert time
                    monitorAlerting.setLastAlertTime(lastAlertTime);
                    updateLastAlertTimeInDatabase(monitorAlerting); // Update the DB with the current time
           //     }
            }
        }
    }

    private boolean shouldSendAlert(fvm_monitoring monitoring) {
        return monitoring.getAktueller_Wert() > monitoring.getError_Schwellwert();
    }

//    private boolean shouldSendEmail(MonitorAlerting monitorAlerting) {
//        // Calculate the next valid email sending time
//        LocalDateTime nextValidTime = lastAlertTime.plusMinutes(monitorAlerting.getIntervall());
//        return LocalDateTime.now().isAfter(nextValidTime);
//    }

    private void sendAlertEmail(MonitorAlerting config, fvm_monitoring monitoring) {

        try {
            emailService.sendAttachMessage(config.getMailEmpfaenger(), config.getMailCCEmpfaenger(), config.getMailBetreff(), config.getMailText());
            Notification.show("email send success!");
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error while sending mail: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void updateLastAlertTimeInDatabase(MonitorAlerting monitorAlerting) {
        try {
            // Assuming there is only one row in the table, remove the WHERE clause
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET LAST_ALERT_TIME = ?";

            // Example with JDBC
            jdbcTemplate.update(updateQuery, LocalDateTime.now());
            logger.info("updateLastAlertTimeInDatabase: Updated last alert time in database");
           // System.out.println("Updated last alert time in database.");
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error while updating last alert time in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            logger.error("updateLastAlertTimeInDatabase");
        }
    }

    private void updateLastAlertCheckTimeInDatabase(MonitorAlerting monitorAlerting) {
        try {
            String updateQuery = "UPDATE ekp.FVM_MONITOR_ALERTING SET LAST_ALERT_CHECKTIME = ?";
            jdbcTemplate.update(updateQuery, LocalDateTime.now());
            logger.info("updateLastAlertCheckTimeInDatabase: Updated last alert check time in database for ID: " + monitorAlerting.getId());
          //  System.out.println("Updated last alert check time in database for ID: " + monitorAlerting.getId());
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error while updating last alert check time in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            logger.error("updateLastAlertCheckTimeInDatabase");
        }
    }
    private void stopCountdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private Duration calculateRemainingTime(Duration duration, Instant startTime) {
        Instant now = Instant.now();
        Instant endTime = startTime.plus(duration);
        return Duration.between(now, endTime);
    }

    private void updateCountdownLabel(Duration remainingTime) {
        long seconds = remainingTime.getSeconds();
        String formattedTime = String.format("%02d", (seconds % 60));

        if (remainingTime.isNegative()){
            startTime = Instant.now();
            updateTreeGrid();
          //  updateGrid();
            updateLastRefreshLabel();
            return;
        }

        countdownLabel.setText("in " + formattedTime + " Sekunden");
    }

    private void startCountdown(Duration duration) {
        executor = Executors.newSingleThreadScheduledExecutor();

        startTime = Instant.now();

        executor.scheduleAtFixedRate(() -> {
            ui.access(() -> {
                Duration remainingTime = calculateRemainingTime(duration, startTime);
                updateCountdownLabel(remainingTime);
            });
        }, 0, 1, TimeUnit.SECONDS);
    }


    private static ComponentRenderer<PersonDetailsFormLayout, fvm_monitoring> createPersonDetailsRenderer() {
        return new ComponentRenderer<>(PersonDetailsFormLayout::new,
                PersonDetailsFormLayout::setPerson);
    }



    private static class PersonDetailsFormLayout extends FormLayout {
        private final TextField lastRefreshField = new TextField("Letzte Aktualisierung");
        private final TextField refreshIntervallField = new TextField("Refresh-Intervall");
        private final TextField idField = new TextField("ID");

        public PersonDetailsFormLayout() {
            Stream.of(idField,refreshIntervallField,lastRefreshField).forEach(field -> {
                field.setReadOnly(true);
                add(field);
            });

            setResponsiveSteps(new ResponsiveStep("0", 3));

        }

        public void setPerson(fvm_monitoring person) {

            if (person.getPid() != null && person.getPid() != 0) {

                idField.setVisible(true);
                refreshIntervallField.setVisible(true);
                lastRefreshField.setVisible(true);

                if (person.getZeitpunkt() != null) {

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    String dateAsString = dateFormat.format(person.getZeitpunkt());

                    lastRefreshField.setValue(dateAsString);
                } else {
                    lastRefreshField.setValue("unbekannt...");
                }
                idField.setValue(person.getID().toString());
                if(person.getCheck_Intervall() != null) {
                    refreshIntervallField.setValue(person.getCheck_Intervall().toString());
                }
            } else {
                idField.setVisible(false);
                refreshIntervallField.setVisible(false);
                lastRefreshField.setVisible(false);
            }
        }
    }

    private static String xmlpare(String xml) {


        String input = "<data><object><property name=\"body\"><object><property name=\"item\"><object><property name=\"name\"><value type=\"string\">AdminServer</value></property><property name=\"state\"><value type=\"string\">RUNNING</value></property><property name=\"health\"><value type=\"string\">HEALTH_OK</value></property><property name=\"clusterName\"><value/></property><property name=\"currentMachine\"><value type=\"string\"></value></property><property name=\"weblogicVersion\"><value type=\"string\">WebLogic Server 12.2.1.4.0 Thu Sep 12 04:04:29 GMT 2019 1974621</value></property><property name=\"openSocketsCurrentCount\"><value type=\"number\">1</value></property><property name=\"heapSizeMax\"><value type=\"number\">518979584</value></property><property name=\"oSVersion\"><value type=\"string\">5.4.0-42-generic</value></property><property name=\"oSName\"><value type=\"string\">Linux</value></property><property name=\"javaVersion\"><value type=\"string\">1.8.0_202</value></property><property name=\"heapSizeCurrent\"><value type=\"number\">284954624</value></property><property name=\"heapFreeCurrent\"><value type=\"number\">76093760</value></property></object></property></object></property><property name=\"messages\"><array></array></property></object></data>";

        // Extract property name-value pairs
        Pattern pattern = Pattern.compile("<property name=\"(.*?)\"><value(.*?)>(.*?)</value></property>");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(3);

            System.out.println("Name: " + name);
            System.out.println("Value: " + value);
            System.out.println();
        }


        return null;

    }



    private class MonitorContextMenu extends GridContextMenu<fvm_monitoring> {
        public MonitorContextMenu(Grid<fvm_monitoring> target) {
            super(target);

            // Description context menu item
            GridMenuItem<fvm_monitoring> beschreibungItem = addItem("Beschreibung", e -> e.getItem().ifPresent(a -> {
                if (a.getPid() != 0) {
                    System.out.printf("Beschreibung: %s%n", a.getID());

                    dialog_Beschreibung.setHeaderTitle(a.getTitel());
                    VerticalLayout dialogLayout = showDialog(a);

                    dialog_Beschreibung.removeAll();
                    dialog_Beschreibung.add(dialogLayout);
                    dialog_Beschreibung.setModal(false);
                    dialog_Beschreibung.setDraggable(true);
                    dialog_Beschreibung.setResizable(true);
                    dialog_Beschreibung.setWidth("800px");
                    dialog_Beschreibung.setHeight("600px");
                    dialog_Beschreibung.open();
                }
            }));

            // Show Data context menu item
            GridMenuItem<fvm_monitoring> showDataItem = addItem("Show Data", e -> e.getItem().ifPresent(a -> {
                if (a.getPid() != 0) {
                    dialog_Beschreibung.setHeaderTitle("Detailabfrage für " + a.getTitel() + " (ID: " + a.getID() + ")");
                    VerticalLayout dialogLayout = null;
                    try {
                        dialogLayout = createDialogData(a.getSQL_Detail());
                    } catch (SQLException | IOException ex) {
                        throw new RuntimeException(ex);
                    }

                    dialog_Beschreibung.removeAll();
                    dialog_Beschreibung.add(dialogLayout);
                    dialog_Beschreibung.setModal(false);
                    dialog_Beschreibung.setDraggable(true);
                    dialog_Beschreibung.setResizable(true);
                    dialog_Beschreibung.open();
                }
            }));

            // History context menu item
            GridMenuItem<fvm_monitoring> historyItem = addItem("Historie", e -> e.getItem().ifPresent(monitor -> {
                if (monitor.getPid() != 0) {
                    dialog_Beschreibung.setHeaderTitle("Historie für " + monitor.getTitel() + " (ID: " + monitor.getID() + ")");
                    VerticalLayout dialogLayout = createDialogGraph(monitor.getID());

                    dialog_Beschreibung.removeAll();
                    dialog_Beschreibung.add(dialogLayout);
                    dialog_Beschreibung.setModal(false);
                    dialog_Beschreibung.setDraggable(true);
                    dialog_Beschreibung.setResizable(true);
                    dialog_Beschreibung.open();
                }
            }));

            // Edit context menu item (not shown when pid == 0)
            GridMenuItem<fvm_monitoring> editItem = addItem("Edit", e -> e.getItem().ifPresent(monitor -> {
                System.out.printf("Edit: %s%n", monitor.getID());
                showEditDialog(monitor, "Edit");
            }));

            // New context menu item for pid == 0 or no selection
            GridMenuItem<fvm_monitoring> newItemForPidZero = addItem("New", e -> {
                if (e.getItem().isPresent()) {
                    fvm_monitoring monitor = e.getItem().get();
                    System.out.println("New dialog open for existing monitor");
                    showEditDialog(monitor, "New");
                } else {
                    // No row selected, open "New" dialog without a monitor instance
                    System.out.println("New dialog open for no selection");
                    showEditDialog(null, "New");  // Pass a new instance for creating a new monitor
                }
            });

            GridMenuItem<fvm_monitoring> deleteItem = addItem("Delete", e -> e.getItem().ifPresent(monitor -> {
                System.out.printf("Delete: %s%n", monitor.getID());
                showDeleteDialog(monitor);
            }));

            // Refresh context menu item
            GridMenuItem<fvm_monitoring> refreshItem = addItem("Refresh", e -> e.getItem().ifPresent(monitor -> {
                if (monitor.getPid() != 0) {
                    logger.debug("Refresh im ContextMenü aufgerufen für ID: " + monitor.getID());

                    if(monitor.getType().contains("Shell-Abfrage"))
                    {
                        logger.info("Ausführen Shell Command");
                        executeImmediateShellCheck(monitor);
                    }
                    else
                    {
                        logger.info("Ausführen SQL Command");
                        executeImmediateSQLCheck(monitor);
                    }


                 //   refreshMonitor(monitor.getID());
                 //   executeImmediateSQLCheck(monitor);
                   // shutdownExecutorService();
                }
            }));

            // Set dynamic content handler to hide other options when pid == 0
            setDynamicContentHandler(person -> {
                // If pid == 0, show only the "Edit" option
                if (person == null) {
                    beschreibungItem.setEnabled(false);
                    showDataItem.setEnabled(false);
                    historyItem.setEnabled(false);
                    editItem.setEnabled(false);
                    refreshItem.setEnabled(false);
                    deleteItem.setEnabled(false);
                    newItemForPidZero.setEnabled(true);
                } else if (person.getPid() == 0) {
                    beschreibungItem.setEnabled(false);
                    showDataItem.setEnabled(false);
                    historyItem.setEnabled(false);
                    editItem.setEnabled(true);
                    refreshItem.setEnabled(false);
                    newItemForPidZero.setEnabled(false);
                    deleteItem.setEnabled(true);
                } else {
                    beschreibungItem.setEnabled(true);
                    showDataItem.setEnabled(true);
                    historyItem.setEnabled(true);
                    editItem.setEnabled(true);
                    refreshItem.setEnabled(true);
                    newItemForPidZero.setEnabled(true);
                    deleteItem.setEnabled(true);
                }
                return true;
            });
        }
    }


    private void refreshMonitor(Integer id) {

        String sql="begin parallel_proc(''," + id + "); end;";
        System.out.println(sql);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        try {

            jdbcTemplate.setDataSource(ds);

            jdbcTemplate.execute(sql);

            System.out.println("Refresh ausgeführt");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        return ;

    }

    public void executeImmediateShellCheck(fvm_monitoring monitoring) {
        UI ui = UI.getCurrent();
        executorService.submit(() -> {
            //    if (monitoring.getIS_ACTIVE().equals("1")) {
            String result = null;
            try {
                  String shellCommand = monitoring.getShellCommand();
                  if(shellCommand != null)
                  {
                      jdbcTemplate = cockpitService.getNewJdbcTemplateWithDatabase(comboBox.getValue());
                     String server = monitoring.getShellServer();
                     ServerConfiguration serverConfiguration = CockpitView.serverConfigurationList.stream()
                              .filter(entity -> entity.getHostAlias().equals(server))
                              .findFirst()
                              .orElse(null);
                     String username = serverConfiguration.getUserName();
                     String host = serverConfiguration.getHostName();
                     SftpClient cl = new SftpClient(host, Integer.parseInt(serverConfiguration.getSshPort()), username);

                     cl.authKey(serverConfiguration.getSshKey(), "");
                     result = cl.executeShellCommand(shellCommand);
                     logger.debug("Result from Shellscript: " + result);
                     if(result==null)
                     {
                         logger.error("Result from Shellscript is null!!!");
                         result="9999";
                     }


                    }

                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM FVM_MONITOR_RESULT WHERE ID = ?",
                        new Object[]{monitoring.getID()},
                        Integer.class
                );
                //    logger.info("Connection: " + jdbcTemplate.getDataSource().getConnection().getMetaData().getUserName() );
                logger.debug("Execute SQL: SELECT COUNT(*) FROM FVM_MONITOR_RESULT WHERE ID =" + monitoring.getID() + "; Result: >" +result +"<") ;

                if (count != null ) {

                    logger.debug("UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID=" + monitoring.getID() + ";");

                    jdbcTemplate.update(
                            "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID = ?",
                            monitoring.getID());
                }
                    logger.debug("INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) " +
                            "VALUES (" + monitoring.getID() + "," + Timestamp.valueOf(LocalDateTime.now())+ ",1, " + result + ",\"Shell-Command " + shellCommand + " executed successfully\")");

                    jdbcTemplate.update(
                            "INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                            monitoring.getID(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            1, // Mark as active
                            result,
                            "Shell-Command " + shellCommand + " executed successfully");

                if (ui != null) {
                    String finalResult = result;
                    ui.access(() -> {
                        Notification.show("refresh : Shell Check ID: " + monitoring.getID() + " result: "+ finalResult, 5000, Notification.Position.MIDDLE);
                    });
                }


            }catch (Exception ex) {
                logger.error(ex.getMessage());

                if (ui != null) {
                    String finalResult = result;
                    ui.access(() -> {
                        Notification.show("refresh : Shell Check ID: " + monitoring.getID() + "Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                    });
                }
                String shellCommand = monitoring.getShellCommand();
                logger.debug("INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) " +
                        "VALUES (" + monitoring.getID() + "," + Timestamp.valueOf(LocalDateTime.now())+ ",0, " + result + ",\"Shell-Command " + shellCommand + " executed successfully\")");

                jdbcTemplate.update(
                        "INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                        monitoring.getID(),
                        Timestamp.valueOf(LocalDateTime.now()),
                        0, // Mark as deactive
                        result,
                        "Error: " + ex.getMessage());
            }finally {
                cockpitService.connectionClose(jdbcTemplate);
            }

        });

    }
    public void executeImmediateSQLCheck(fvm_monitoring monitoring) {
        UI ui = UI.getCurrent();
        executorService.submit(() -> {
        //    if (monitoring.getIS_ACTIVE().equals("1")) {
            String result = null;
                try {
                    if(monitoring.getType().contains("SQL")) {
                        jdbcTemplate = cockpitService.getNewJdbcTemplateWithDatabase(comboBox.getValue());
                        String sqlQuery = monitoring.getSQL();
                        result = jdbcTemplate.queryForObject(sqlQuery, String.class);
                    } else {
                        String shellCommand = monitoring.getShellCommand();
                        if(shellCommand != null) {
                            String server = monitoring.getShellServer();
                            ServerConfiguration serverConfiguration = CockpitView.serverConfigurationList.stream()
                                    .filter(entity -> entity.getHostAlias().equals(server))
                                    .findFirst()
                                    .orElse(null);
                            String username = serverConfiguration.getUserName();
                            String host = serverConfiguration.getHostName();
                            SftpClient cl = new SftpClient(host, Integer.parseInt(serverConfiguration.getSshPort()), username);
                            cl.authKey(serverConfiguration.getSshKey(), "");
                            result = cl.executeBackgroundShellCommand(shellCommand);

                        }
                    }


                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM FVM_MONITOR_RESULT WHERE ID = ?",
                            new Object[]{monitoring.getID()},
                            Integer.class
                    );
                //    logger.info("Connection: " + jdbcTemplate.getDataSource().getConnection().getMetaData().getUserName() );
                    logger.info("Execute SQL: SELECT COUNT(*) FROM FVM_MONITOR_RESULT WHERE ID =" + monitoring.getID() + "; Result: >" +result +"<") ;

                    if (count != null ) {

                        logger.debug("UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID=" + monitoring.getID() + ";");

                        jdbcTemplate.update(
                                "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID = ?",
                                monitoring.getID());
                    }
                        logger.debug("INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) " +
                                "VALUES (" + monitoring.getID() + "," + Timestamp.valueOf(LocalDateTime.now()) + ",1, " + result + ",\"Query executed successfully\")");

                        jdbcTemplate.update(
                                "INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                                monitoring.getID(),
                                Timestamp.valueOf(LocalDateTime.now()),
                                1, // Mark as active
                                result,
                                "Query executed successfully");

                    if (ui != null) {
                        String finalResult = result;
                        ui.access(() -> {
                            Notification.show("refresh : sql check ID: " + monitoring.getID() + " result: "+ finalResult, 5000, Notification.Position.MIDDLE);
                        });
                    }

                } catch (Exception ex) {
                    logger.error(ex.getMessage());

                    logger.debug("INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) " +
                            "VALUES (" + monitoring.getID() + "," + Timestamp.valueOf(LocalDateTime.now())+ ",0, " + result + ", \"Error:" + ex.getMessage() +"\")");

                    jdbcTemplate.update(
                            "INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                            monitoring.getID(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            0, // Mark as deactive
                            result,
                            "Error: " + ex.getMessage());

                    //System.out.println("Erorr: while " + monitoring.getID() + "----------------------query executed: " + monitoring.getSQL().toString());
                    if (ui != null) {
                        ui.access(() -> {
                            Notification.show("Erorr: while sql-check: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        });
                    } else {
                        System.err.println("No active UI context found. Unable to show notification.");
                    }
                } finally {
                    cockpitService.connectionClose(jdbcTemplate);
                //}
            }
        });
    }
    public void shutdownExecutorService() {
        executorService.shutdown();
    }

    private void fill_grid_metadata(String sql) throws SQLException, IOException {
        System.out.println(sql);
        // Create the grid and set its items
        //Grid<LinkedHashMap<String, Object>> grid2 = new Grid<>();
        grid_metadata.removeAllColumns();

        //List<LinkedHashMap<String,Object>> rows = retrieveRows("select * from EKP.ELA_FAVORITEN where rownum<200");
        List<LinkedHashMap<String,Object>> rows = retrieveRows(sql);

        if(!rows.isEmpty()){
            grid_metadata.setItems( rows); // rows is the result of retrieveRows

            // Add the columns based on the first row
            LinkedHashMap<String, Object> s = rows.get(0);
            for (Map.Entry<String, Object> entry : s.entrySet()) {
                grid_metadata.addColumn(h -> h.get(entry.getKey().toString())).setHeader(entry.getKey()).setAutoWidth(true).setResizable(true).setSortable(true);
            }

            grid_metadata.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
            grid_metadata.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            grid_metadata.addThemeVariants(GridVariant.LUMO_COMPACT);
            //   grid2.setAllRowsVisible(true);
            grid_metadata.setPageSize(50);
            grid_metadata.setHeight("800px");

            grid_metadata.getStyle().set("resize", "vertical");
            grid_metadata.getStyle().set("overflow", "auto");

            //grid2.setPaginatorSize(5);
            // Add the grid to the page

            this.setPadding(false);
            this.setSpacing(false);
            this.setBoxSizing(BoxSizing.CONTENT_BOX);

        }
        else {
            //Text txt = new Text("Es konnten keine Daten  abgerufen werden!");
            //add(txt);
        }

    }

    public List<LinkedHashMap<String, Object>> retrieveRows(String queryString) {
        List<LinkedHashMap<String, Object>> rows = new LinkedList<>();
        Notification notification;
        if (queryString != null) {
            try {
                // Get JdbcTemplate using the provided configuration
                Configuration conf = comboBox.getValue();
                JdbcTemplate jdbcTemplate = cockpitService.getNewJdbcTemplateWithDatabase(conf);

                if (jdbcTemplate == null) {
                    logger.error("retrieveRows: Failed to create JdbcTemplate");
                    throw new SQLException("Failed to create JdbcTemplate.");
                }
                logger.info("retrieveRows: for show data");
                // Execute the query and map the result set to rows
                jdbcTemplate.query(queryString, (ResultSet rs) -> {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnLabel(i);
                            Object value = rs.getObject(i) == null ? "" : rs.getObject(i);
                            row.put(columnName, value);
                        }
                        rows.add(row); // Add directly to the rows list
                    }
                    return null; // Return null as we're not using the ResultSetExtractor's result
                });
            } catch (SQLException e) {
                logger.error("retrieveRows: "+e.getMessage());
                notification = Notification.show(e.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return Collections.emptyList();
            } finally {
                cockpitService.connectionClose(jdbcTemplate);
            }
        } else {
            logger.info("retrieveRows: SQL is null");
            notification = Notification.show("SQL is null");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return rows;
    }

    private VerticalLayout createDialogData(String sql) throws SQLException, IOException {

        //fill_grid_metadata("select nachrichtidintern,nachrichtidextern,status,art,eingangsdatumserver,fehlertag,verarbeitet,loeschtag,senderpostfachname,empfaengeraktenzeichen from ekp.metadaten\n where rownum < 5");
        fill_grid_metadata(sql);

        VerticalLayout dialogLayout = new VerticalLayout(grid_metadata);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "1200px").set("max-width", "100%");
        dialogLayout.getStyle().set("height", "800px").set("max-height", "100%");
        return dialogLayout;
    }
    private VerticalLayout createDialogGraph(Integer id) {

        Chart chart = new Chart();
        chart.getConfiguration().getChart().setType(ChartType.SPLINE);

        com.vaadin.flow.component.charts.model.Configuration conf = chart.getConfiguration();
        // Configuration conf = chart.getConfiguration();

        conf.getxAxis().setType(AxisType.DATETIME);

        DataSeries series = new DataSeries("Zeit");
      /*  series.add(new DataSeriesItem(new Date(2023, 5, 1,10,30), 200));
        series.add(new DataSeriesItem(new Date(2023, 5, 1,11,35), 210));
        series.add(new DataSeriesItem(new Date(2023, 5, 1,12,40), 280));
        series.add(new DataSeriesItem(new Date(2023, 5, 1,14,45), 290));
        series.add(new DataSeriesItem(new Date(2023, 5, 1,15,50), 100));

        series.add(new DataSeriesItem(new Date(2023, 5, 2,10,30), 120));
        series.add(new DataSeriesItem(new Date(2023, 5, 2,11,35), 150));
        series.add(new DataSeriesItem(new Date(2023, 5, 2,12,40), 180));
        series.add(new DataSeriesItem(new Date(2023, 5, 2,15,45), 290));
        series.add(new DataSeriesItem(new Date(2023, 5, 2,16,50), 310));

        series.add(new DataSeriesItem(new Date(2023, 5, 3,10,30), 120));
        series.add(new DataSeriesItem(new Date(2023, 5, 3,11,35), 150));
        series.add(new DataSeriesItem(new Date(2023, 5, 3,12,40), 280));
        series.add(new DataSeriesItem(new Date(2023, 5, 3,16,45), 290));
        series.add(new DataSeriesItem(new Date(2023, 5, 3,18,50), 500));
        series.add(new DataSeriesItem(new Date(2023, 5, 3,20,50), 600));*/

        List<fvm_monitoring> ll =  getHistMonitoring(id);

        ll.forEach(eintrag -> {
            System.out.println(eintrag.getZeitpunkt() + ": " + eintrag.getAktueller_Wert());
            java.util.Date z = eintrag.getZeitpunkt(); // oder Timestamp/Date je nach Entity
            Instant instant = z.toInstant();
            series.add(new DataSeriesItem(instant, eintrag.getAktueller_Wert()));
        });

        conf.addSeries(series);

        YAxis yaxis = new YAxis();
        yaxis.setTitle("Anzahl");
        conf.addyAxis(yaxis);

        VerticalLayout dialogLayout = new VerticalLayout(chart);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "1200px").set("max-width", "100%");
        dialogLayout.getStyle().set("height", "800px").set("max-height", "100%");
        return dialogLayout;
    }



    private static VerticalLayout showDialog(fvm_monitoring Inhalt){

        VerticalLayout dialogInhalt = new VerticalLayout();

        details = new Tab("Beschreibung");
        payment = new Tab("Handlungsanweisung");
        //data = new Tab("Daten");

        Tabs tabs = new Tabs();

        tabs.addSelectedChangeListener(
                event -> setContent(event.getSelectedTab(),Inhalt));


        tabs.add(details, payment);


        content.setSpacing(false);
        content.add(tabs);

        setContent(tabs.getSelectedTab(),Inhalt);

        dialogInhalt = new VerticalLayout();
        dialogInhalt.add(tabs,content);

        return dialogInhalt;

    }

    private VerticalLayout showEditDialog(fvm_monitoring monitor, String context){
        VerticalLayout dialogLayout = new VerticalLayout();
        Dialog dialog = new Dialog();

        boolean isNew = false;
        fvm_monitoring newMonitor = new fvm_monitoring();

        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("1000px");
        dialog.setHeight("600px");

        if(context.equals("New")){
            if(monitor != null) {
                if (monitor.getPid() == 0) {
                    newMonitor.setPid(monitor.getID());
                    dialog.add(getParentNodeDialog(newMonitor, true));

                } else {
                    newMonitor.setPid(monitor.getPid());
                    dialog.add(getTabsheet(newMonitor, true));
                }
            } else  {
                dialog.add(getParentNodeDialog(newMonitor, true));
                dialog.setWidth("300px");
                dialog.setHeight("250px");
            }

        } else {
            if (monitor.getPid() == 0) {
                dialog.add(getParentNodeDialog(monitor, false));
                dialog.setWidth("300px");
                dialog.setHeight("250px");
            } else {
                dialog.add(getTabsheet(monitor, false));
            }
        }

        //  Button addButton = new Button("add");
        Button cancelButton = new Button("Cancel");
        Button saveButton = new Button(context.equalsIgnoreCase("New") ? "Add" : "Save");
        // Add buttons to the footer
        dialog.getFooter().add(saveButton, cancelButton);

        cancelButton.addClickListener(cancelEvent -> {
            dialog.close(); // Close the confirmation dialog
        });

        saveButton.addClickListener(saveEvent -> {
            System.out.println("saved data....");
            if(context.equals("New")) {
                saveEditedMonitor(newMonitor);
            } else {
                saveEditedMonitor(monitor);
            }

            updateTreeGrid();
         //    updateGrid();
            dialog.close(); // Close the confirmation dialog
        });

        dialog.open();

        return dialogLayout;

    }

    private void showDeleteDialog(fvm_monitoring monitor) {
        Dialog confirmationDialog = new Dialog();
        confirmationDialog.setHeaderTitle("Confirm Deletion");

        String name = monitor.getTitel();
        if(name == null) {
            name = monitor.getBereich();
        }
        Span confirmationMessage =
                new Span("Are you sure you want to delete the entry: " + name + "?");
        confirmationDialog.add(confirmationMessage);

        Button confirmButton = new Button("Delete", event -> {
            if (cockpitService.hasChildEntries(monitor)) {
                Notification.show("Cannot delete. This entry has child elements!", 3000, Notification.Position.MIDDLE);
            } else {
                cockpitService.deleteMonitor(monitor, comboBox.getValue());
                updateTreeGrid();
            }
            confirmationDialog.close();
        });

        Button cancelButton = new Button("Cancel", event -> confirmationDialog.close());

        confirmButton.getStyle().set("color", "red");
        cancelButton.getStyle().set("color", "blue");

        HorizontalLayout buttonLayout = new HorizontalLayout(confirmButton, cancelButton);
        confirmationDialog.getFooter().add(buttonLayout);

        confirmationDialog.open();
    }

    private TabSheet getTabsheet(fvm_monitoring monitor, boolean isNew) {

        TabSheet tabSheet = new TabSheet();

        tabSheet.add("General", getGeneral(monitor, isNew));
        tabSheet.add("Abfrage", getSqlAbfrage(monitor, isNew));
        tabSheet.add("Beschreibung", getBeschreibung(monitor, isNew));
        tabSheet.add("Handlungsinformationen", getHandlungsinformationen(monitor, isNew));

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();

        return tabSheet;
    }

    private void saveEditedMonitor(fvm_monitoring monitor) {
        callback.save(monitor);
        restartBackgroundCron();
    }

    private Component getHandlungsinformationen(fvm_monitoring monitor, boolean isNew) {
        VerticalLayout content = new VerticalLayout();

        Button saveBtn = new Button("save");
        Button editBtn = new Button("edit");
        saveBtn.setVisible(false);
        editBtn.setVisible(true);

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withType(CKEditorType.CLASSIC)
                .withPreset(CKEditorPreset.STANDARD)
                .withTheme(CKEditorTheme.AUTO)
                .withValue(isNew ? "" : (monitor.getBeschreibung() != null ? monitor.getBeschreibung() : ""))
                .build();

        editor.setWidth("95%");
        editor.getStyle().setMargin("-5px");
        editor.addValueChangeListener(event -> monitor.setBeschreibung(event.getValue()));

        //    editor.setReadOnly(true);
        editor.getStyle().setMargin ("-5px");
        //  editor.setValue(monitor.getHandlungs_INFO());
        editor.setValue(isNew ? "" : (monitor.getHandlungs_INFO() != null ? monitor.getHandlungs_INFO() : ""));
        editor.addValueChangeListener(event -> monitor.setHandlungs_INFO(event.getValue()));
//        saveBtn.addClickListener((event -> {
//            editBtn.setVisible(true);
//            saveBtn.setVisible(false);
//            //editor.setReadOnly(true);
//            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
//
//        }));
//
//        editBtn.addClickListener(e->{
//            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
//            editBtn.setVisible(false);
//            saveBtn.setVisible(true);
//            //editor.setReadOnly(false);
//        });

//        content.add(editor,editBtn,saveBtn);
        content.add(editor);
        return content;
    }

    private Component getBeschreibung(fvm_monitoring monitor, boolean isNew) {
        VerticalLayout content = new VerticalLayout();

        Button saveBtn = new Button("save");
        Button editBtn = new Button("edit");
        saveBtn.setVisible(false);
        editBtn.setVisible(true);

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withType(CKEditorType.CLASSIC)
                .withPreset(CKEditorPreset.STANDARD)
                .withTheme(CKEditorTheme.AUTO)
                .withValue(isNew ? "" : (monitor.getHandlungs_INFO() != null ? monitor.getHandlungs_INFO() : ""))
                .build();

        editor.setWidth("95%");
        editor.getStyle().setMargin("-5px");
        editor.addValueChangeListener(event -> monitor.setHandlungs_INFO(event.getValue()));

        // editor.setReadOnly(true);
        editor.getStyle().setMargin ("-5px");
     //   editor.setValue(monitor.getBeschreibung());
        editor.setValue(isNew ? "" : (monitor.getBeschreibung() != null ? monitor.getBeschreibung() : ""));
        editor.addValueChangeListener(event -> monitor.setBeschreibung(event.getValue()));
//        saveBtn.addClickListener((event -> {
//            editBtn.setVisible(true);
//            saveBtn.setVisible(false);
//            //editor.setReadOnly(true);
//            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
//
//        }));
//
//        editBtn.addClickListener(e->{
//            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
//            editBtn.setVisible(false);
//            saveBtn.setVisible(true);
//            //editor.setReadOnly(false);
//        });

//        content.add(editor,editBtn,saveBtn);
        content.add(editor);
        return content;
    }

    private Component getSqlAbfrage(fvm_monitoring monitor, boolean isNew) {
        VerticalLayout content = new VerticalLayout();
        RadioButtonGroup<String> radioGroup = new RadioButtonGroup<>();
        radioGroup.setLabel("Typ");
        radioGroup.setItems("SQL-Abfrage", "Shell-Abfrage");
        radioGroup.setValue(isNew ? "SQL-Abfrage" : (monitor.getType() != null ? monitor.getType() : ""));
        add(radioGroup);

        content.add(radioGroup);


        TextArea abfrage = new TextArea("SQL-Abfrage");
      //  abfrage.setValue(monitor.getSQL());
        abfrage.setValue(isNew ? "" : (monitor.getSQL() != null ? monitor.getSQL() : ""));
        abfrage.setWidthFull();

        TextArea detailabfrage = new TextArea("SQL-Detail Abfrage");
        if (monitor.getSQL_Detail() != null) {
           // detailabfrage.setValue(monitor.getSQL_Detail());
            detailabfrage.setValue(isNew ? "" : (monitor.getSQL_Detail() != null ? monitor.getSQL_Detail() : ""));
        }
        detailabfrage.setWidthFull();

        TextField shellCommand = new TextField("Command");
        shellCommand.setValue(isNew ? "" : (monitor.getShellCommand() != null ? monitor.getShellCommand() : ""));
        shellCommand.setWidthFull();
        ComboBox<ServerConfiguration> shellConfiguration = new ComboBox<>("Server");
        shellConfiguration.setItems(serverConfigurationList);
        ServerConfiguration serverConfiguration = serverConfigurationList.stream()
                .filter(entity -> entity.getHostAlias().equals(monitor.getShellServer()))
                .findFirst()
                .orElse(null);
        shellConfiguration.setValue(isNew ? null : serverConfiguration);
        shellConfiguration.setItemLabelGenerator(ServerConfiguration::getHostAlias);
        shellConfiguration.setWidthFull();

        abfrage.addValueChangeListener(event -> monitor.setSQL(event.getValue()));
        detailabfrage.addValueChangeListener(event -> monitor.setSQL_Detail(event.getValue()));
        shellCommand.addValueChangeListener(event -> monitor.setShellCommand(event.getValue()));
        shellConfiguration.addValueChangeListener(event -> monitor.setShellServer(event.getValue() != null ? event.getValue().getHostAlias() : null));
        radioGroup.addValueChangeListener(event -> monitor.setType(event.getValue()));
        radioGroup.addValueChangeListener(event -> {
            String selectedType = event.getValue();
            monitor.setType(selectedType);

            boolean isSQLSelected = "SQL-Abfrage".equals(selectedType);
            abfrage.setVisible(isSQLSelected);
            detailabfrage.setVisible(isSQLSelected);
            shellCommand.setVisible(!isSQLSelected);
            shellConfiguration.setVisible(!isSQLSelected);
        });

        boolean isSQL = "SQL-Abfrage".equals(radioGroup.getValue());
        abfrage.setVisible(isSQL);
        detailabfrage.setVisible(isSQL);
        shellCommand.setVisible(!isSQL);
        shellConfiguration.setVisible(!isSQL);

        content.add(abfrage, detailabfrage, shellConfiguration , shellCommand);
        return content;
    }

    private Component getGeneral(fvm_monitoring monitor, boolean isNew) {
        VerticalLayout content = new VerticalLayout();

        IntegerField id = new IntegerField("Id");
        id.setValue(monitor.getID() != null ? monitor.getID() : null);
        id.setReadOnly(true);

        TextField titel = new TextField("Titel");
        titel.setValue(isNew ? "" : (monitor.getTitel() != null ? monitor.getTitel() : ""));
        titel.setWidthFull();

        IntegerField intervall = new IntegerField("Check-Intervall");
        intervall.setValue(isNew ? null : monitor.getCheck_Intervall());

        IntegerField infoSchwellwert = new IntegerField("Warning Schwellwert");
        infoSchwellwert.setValue(isNew ? null : monitor.getWarning_Schwellwert());

        IntegerField errorSchwellwert = new IntegerField("Error Schwellwert");
        errorSchwellwert.setValue(isNew ? null : monitor.getError_Schwellwert());

        IntegerField retentionTime = new IntegerField("Retention Time");
        retentionTime.setValue(isNew ? null : monitor.getRetentionTime());

//        TextField bereich = new TextField("Bereich");
//        bereich.setValue(isNew ? "" : (monitor.getBereich() != null ? monitor.getBereich() : ""));

        Checkbox checkbox = new Checkbox("aktiv");
       // checkbox.setValue(!isNew && monitor.getIS_ACTIVE().equals("1"));
        if (isNew) {
            checkbox.setValue(false);// Set to 0 when new
            monitor.setIS_ACTIVE("0");
        } else {
            checkbox.setValue(monitor.getIS_ACTIVE().equals("1")); // Set based on monitor's IS_ACTIVE
        }

        List<fvm_monitoring> parentNodes = cockpitService.getParentNodes();
        ComboBox<fvm_monitoring> parentComboBox = new ComboBox<>("Parent Node (PID)");
        parentComboBox.setItems(parentNodes);  // Populate with parent options
        parentComboBox.setItemLabelGenerator(fvm_monitoring::getBereich);
        if(monitor.getPid() != null) {
            if(monitor.getPid() == 0){
                parentComboBox.setEnabled(false);
            }
            parentComboBox.setValue(cockpitService.getParentByPid(monitor.getPid()));
        } else {
            monitor.setPid(parentNodes.get(0).getID());
            parentComboBox.setValue(parentNodes.get(0));
        }

        // Add value change listeners to trigger binder updates
        titel.addValueChangeListener(event -> monitor.setTitel(event.getValue()));
   //     bereich.addValueChangeListener(event -> monitor.setBereich(event.getValue()));
        intervall.addValueChangeListener(event -> monitor.setCheck_Intervall(event.getValue()));
        retentionTime.addValueChangeListener(event -> monitor.setRetentionTime(event.getValue()));
        infoSchwellwert.addValueChangeListener(event -> monitor.setWarning_Schwellwert(event.getValue()));
        errorSchwellwert.addValueChangeListener(event -> monitor.setError_Schwellwert(event.getValue()));
        checkbox.addValueChangeListener(event -> monitor.setIS_ACTIVE(event.getValue() ? "1" : "0"));
        parentComboBox.addValueChangeListener(event -> monitor.setPid(event.getValue().getID()));

        HorizontalLayout hr = new HorizontalLayout(intervall,infoSchwellwert,errorSchwellwert, checkbox, retentionTime);
        hr.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        HorizontalLayout hr1 = new HorizontalLayout(id,parentComboBox);
        hr1.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        content.add(hr1, titel, hr);
        return content;
    }

    private Component getParentNodeDialog(fvm_monitoring monitor, boolean isNew) {
        VerticalLayout content = new VerticalLayout();

        TextField bereich = new TextField("Bereich");
        bereich.setValue(isNew ? "" : (monitor.getBereich() != null ? monitor.getBereich() : ""));

        monitor.setPid(0);
        monitor.setIS_ACTIVE("0");
        // Add value change listeners to trigger binder updates
        bereich.addValueChangeListener(event -> monitor.setBereich(event.getValue()));
        content.add(bereich);
        return content;
    }

    private void emailConfigurationDialog() {
        // Create a dialog with a header title
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("E-Mail Konfiguration");

        // Create fields for user input
        TextField mailEmpfaengerField = new TextField("MAIL_EMPFAENGER");
        TextField mailCCEmpfaengerField = new TextField("MAIL_CC_EMPFAENGER");
        TextField mailBetreffField = new TextField("MAIL_BETREFF");
        TextArea mailTextArea = new TextArea("MAIL_TEXT");
    //    TextField cronField = new TextField("CRON_EXPRESSION");
      //  IntegerField intervalField = new IntegerField("Intervall (in minutes)");
        Checkbox aktiv = new Checkbox("aktiv");

        // Set widths for fields
        mailEmpfaengerField.setWidth("100%");
        mailCCEmpfaengerField.setWidth("100%");
        mailBetreffField.setWidth("100%");
        mailTextArea.setWidth("100%");
        mailTextArea.setHeight("150px"); // Adjust height as needed
    //    cronField.setWidth("100%");
        aktiv.setWidth("100%");

        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(comboBox.getValue());

        Optional.ofNullable(monitorAlerting.getMailEmpfaenger()).ifPresent(mailEmpfaengerField::setValue);
        Optional.ofNullable(monitorAlerting.getMailCCEmpfaenger()).ifPresent(mailCCEmpfaengerField::setValue);
        Optional.ofNullable(monitorAlerting.getMailBetreff()).ifPresent(mailBetreffField::setValue);
        Optional.ofNullable(monitorAlerting.getMailText()).ifPresent(mailTextArea::setValue);
    //    Optional.ofNullable(monitorAlerting.getCron()).ifPresent(cronField::setValue);
        aktiv.setValue(monitorAlerting.getIsActive() != null && monitorAlerting.getIsActive() != 0);

        Button saveButton = new Button("Save", event -> {
            // Update the monitorAlerting object with values from the input fields
            monitorAlerting.setMailEmpfaenger(mailEmpfaengerField.getValue());
            monitorAlerting.setMailCCEmpfaenger(mailCCEmpfaengerField.getValue());
            monitorAlerting.setMailBetreff(mailBetreffField.getValue());
            monitorAlerting.setMailText(mailTextArea.getValue());
    //        monitorAlerting.setCron(cronField.getValue());
            monitorAlerting.setIsActive(aktiv.getValue() ? 1: 0);

            // Call the save method to persist the configuration
            boolean isSuccess = cockpitService.saveEmailConfiguration(monitorAlerting, comboBox.getValue());
            if(isSuccess) {
                if(aktiv.getValue()) {
                    setAlerting("On");
                } else {
                    setAlerting("Off");
                }
                restartAlertCron();
            }

            dialog.close(); // Close the dialog after saving
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY); // Apply primary theme

        Button cancelButton = new Button("Cancel", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY); // Apply tertiary theme

        // Create layout and add components
        VerticalLayout layout = new VerticalLayout(
                mailEmpfaengerField,
                mailCCEmpfaengerField,
                mailBetreffField,
                mailTextArea,
   //             cronField,
                aktiv,
                new HorizontalLayout(saveButton, cancelButton) // Align buttons horizontally
        );
        layout.setSpacing(true); // Add spacing between components
        layout.setPadding(true); // Add padding around the layout
        layout.setMargin(true); // Add margin around the layout
        layout.setWidth("500px"); // Set a fixed width for the layout

        // Add layout to dialog
        dialog.add(layout);
        dialog.setWidth("600px"); // Set a fixed width for the dialog
        dialog.open(); // Open the dialog
    }

    private void backGroundConfigurationDialog() {
        // Create a dialog with a header title
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Background Job Konfiguration");

        // Create fields for user input
        TextField cronField = new TextField("CRON_EXPRESSION");
        //  IntegerField intervalField = new IntegerField("Intervall (in minutes)");
        Checkbox aktiv = new Checkbox("BackJob aktiv");
        IntegerField retentionTimeField = new IntegerField("RETENTION_TIME");
        IntegerField maxParallelChecksField = new IntegerField("MAX_PARALLEL_CHECKS");

        cronField.setWidth("100%");
        aktiv.setWidth("100%");
        retentionTimeField.setWidth("100%");
        maxParallelChecksField.setWidth("100%");

        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(comboBox.getValue());

        Optional.ofNullable(monitorAlerting.getBgCron()).ifPresent(cronField::setValue);
        aktiv.setValue(monitorAlerting.getIsBackJobActive() != null && monitorAlerting.getIsBackJobActive() != 0);
        Optional.ofNullable(monitorAlerting.getRetentionTime()).ifPresent(retentionTimeField::setValue);
        Optional.ofNullable(monitorAlerting.getMaxParallelCheck()).ifPresent(maxParallelChecksField::setValue);

        Button saveButton = new Button("Save", event -> {
            // Update the monitorAlerting object with values from the input fields
            monitorAlerting.setBgCron(cronField.getValue());
            monitorAlerting.setIsBackJobActive(aktiv.getValue() ? 1: 0);
            monitorAlerting.setRetentionTime(retentionTimeField.getValue());
            monitorAlerting.setMaxParallelCheck(maxParallelChecksField.getValue());

            // Call the save method to persist the configuration
            boolean isSuccess = cockpitService.saveBackgoundJobConfiguration(monitorAlerting, comboBox.getValue());
            if(isSuccess) {
                if(aktiv.getValue()) {
                    setChecker("On");
                } else {
                    setChecker("Off");
                }
                restartBackgroundCron();
            }

            dialog.close(); // Close the dialog after saving
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY); // Apply primary theme

        Button cancelButton = new Button("Cancel", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY); // Apply tertiary theme

        // Create layout and add components
        VerticalLayout layout = new VerticalLayout(
                cronField,
                aktiv,
                retentionTimeField,
                maxParallelChecksField,
                new HorizontalLayout(saveButton, cancelButton) // Align buttons horizontally
        );
        layout.setSpacing(true); // Add spacing between components
        layout.setPadding(true); // Add padding around the layout
        layout.setMargin(true); // Add margin around the layout
        layout.setWidth("500px"); // Set a fixed width for the layout

        // Add layout to dialog
        dialog.add(layout);
        dialog.setWidth("600px"); // Set a fixed width for the dialog
        dialog.open(); // Open the dialog
    }

    private void saveEmailConfigurationOld(MonitorAlerting monitorAlerting) {
        try {
//            DriverManagerDataSource ds = new DriverManagerDataSource();
//            com.example.application.data.entity.Configuration conf;
//            conf = comboBox.getValue();
//
//            ds.setUrl(conf.getDb_Url());
//            ds.setUsername(conf.getUserName());
//            ds.setPassword(Configuration.decodePassword(conf.getPassword()));
//
//            jdbcTemplate.setDataSource(ds);
            jdbcTemplate.update("DELETE FROM FVM_MONITOR_ALERTING");
            jdbcTemplate.update(
                    "INSERT INTO FVM_MONITOR_ALERTING (MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, CRON_EXPRESSION) VALUES (?, ?, ?, ?, ?)",
                    monitorAlerting.getMailEmpfaenger(),
                    monitorAlerting.getMailCCEmpfaenger(),
                    monitorAlerting.getMailBetreff(),
                    monitorAlerting.getMailText(),
                    monitorAlerting.getBgCron()
            );
            Notification.show("Configuration saved successfully.");
        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
            Notification.show("Failed to save configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }


    private void restartAlertCron() {
        try {
            Configuration configuration = comboBox.getValue();
            if(alertingState.equals("On")) {
                stopAllScheduledJobs(configuration);
                scheduleEmailMonitorJob(configuration);
            } else {
                stopAllScheduledJobs(configuration);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Failed to restart alert job: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void restartBackgroundCron() {
        try {
            Configuration configuration = comboBox.getValue();
            if(syscheck.getText().equals("On")) {
                stopBackgroundScheduledJobs(configuration);
                scheduleBackgroundJob(configuration);
            } else {
                stopBackgroundScheduledJobs(configuration);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Failed to restart alert job: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }


    private MonitorAlerting fetchEmailConfiguration() {
        MonitorAlerting monitorAlerting = new MonitorAlerting();
        try {

            DriverManagerDataSource ds = new DriverManagerDataSource();
            Configuration conf;
            conf = comboBox.getValue();

            ds.setUrl(conf.getDb_Url());
            ds.setUsername(conf.getUserName());
            ds.setPassword(Configuration.decodePassword(conf.getPassword()));

            jdbcTemplate.setDataSource(ds);

            // Query to get the existing configuration
            String sql = "SELECT MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, CRON_EXPRESSION FROM FVM_MONITOR_ALERTING";

            // Use jdbcTemplate to query and map results to MonitorAlerting object
            jdbcTemplate.query(sql, rs -> {
                // Set the values in the MonitorAlerting object from the result set
                monitorAlerting.setMailEmpfaenger(rs.getString("MAIL_EMPFAENGER"));
                monitorAlerting.setMailCCEmpfaenger(rs.getString("MAIL_CC_EMPFAENGER"));
                monitorAlerting.setMailBetreff(rs.getString("MAIL_BETREFF"));
                monitorAlerting.setMailText(rs.getString("MAIL_TEXT"));
                monitorAlerting.setBgCron(rs.getString("CRON_EXPRESSION"));
            });

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Failed to load configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
        return monitorAlerting;
    }

    private static VerticalLayout showEditDialogOld(fvm_monitoring Inhalt){

        VerticalLayout dialogInhalt = new VerticalLayout();
        TextField titel = new TextField("Titel");
        titel.setValue(Inhalt.getTitel());
        titel.setWidthFull();

        NumberField intervall = new NumberField("Intervall");
        NumberField infoSchwellwert = new NumberField("Warnung-Schwellert");
        NumberField errorSchwellwert = new NumberField("Fehler-Schwellwert");
        Checkbox checkbox = new Checkbox("aktiv");

        HorizontalLayout hr = new HorizontalLayout(intervall,infoSchwellwert,errorSchwellwert, checkbox);
        hr.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        TextArea sql_text = new TextArea ("SQL-Abfrage");
        sql_text.setWidthFull();

        Span descriptionLb = new Span("Beschreibung");
        RichTextEditor rte_Beschreibung = new RichTextEditor();
        RichTextEditor rte_Handlungsanweisung = new RichTextEditor();


        dialogInhalt = new VerticalLayout();
        dialogInhalt.add(titel, hr, sql_text, descriptionLb, rte_Beschreibung, rte_Handlungsanweisung, content);

        return dialogInhalt;

    }


    private static void setContent(Tab tab,fvm_monitoring inhalt ) {

        if(content != null ) {
            content.removeAll();
        }

        if (tab == null) {
            return;
        }
        if (tab.equals(details)) {

            RichTextEditor rte = new RichTextEditor();
            //rte.setMaxHeight("400px");
            //rte.setMinHeight("200px");
            rte.setWidthFull();
            rte.setHeightFull();
            rte.setReadOnly(true);
            rte.addThemeVariants(RichTextEditorVariant.LUMO_NO_BORDER);
            String beschreibung = inhalt.getBeschreibung() != null ? inhalt.getBeschreibung() : "";
            rte.asHtml().setValue(beschreibung);

            //content.add(inhalt.getBeschreibung());
            content.add(rte);
        } else if (tab.equals(payment)) {
            RichTextEditor rte = new RichTextEditor();
            //rte.setMaxHeight("400px");
            //rte.setMinHeight("200px");
            rte.setWidthFull();
            rte.setHeightFull();
            rte.setReadOnly(true);
            rte.addThemeVariants(RichTextEditorVariant.LUMO_NO_BORDER);
            String handlungsInfo = inhalt.getHandlungs_INFO() != null ? inhalt.getHandlungs_INFO() : "";
            rte.asHtml().setValue(handlungsInfo);

            //content.add(inhalt.getBeschreibung());
            content.add(rte);

            //content.add(inhalt.getHandlungs_INFO());

        } else {
            content.add(new Paragraph("ToDo"));
        }
    }


    private static VerticalLayout createDialogLayout(String Beschreibung) {

        RichTextEditor rte = new RichTextEditor();
        //rte.setMaxHeight("400px");
        //rte.setMinHeight("200px");
        rte.setWidthFull();
        rte.setHeightFull();
        rte.setReadOnly(true);
        rte.addThemeVariants(RichTextEditorVariant.LUMO_NO_BORDER);
        rte.asHtml().setValue(Beschreibung);


        VerticalLayout dialogLayout = new VerticalLayout(rte);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "1200px").set("max-width", "100%");
        dialogLayout.getStyle().set("height", "800px").set("max-height", "100%");


        return dialogLayout;
    }

    private List<fvm_monitoring> getMonitoring() {


        //String sql = "SELECT ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT, ERROR_SCHWELLWERT FROM EKP.FVM_MONITORING";

//        String sql = "SELECT m.ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT" +
//                ", ERROR_SCHWELLWERT,mr.result as Aktueller_Wert, 100 / Error_schwellwert * case when mr.result>=Error_schwellwert then Error_Schwellwert else mr.result end  / 100 as Error_Prozent" +
//                ", Zeitpunkt, m.is_active, nvl(m.sql_detail,'select ''Detail-SQL nicht definiert'' from dual') as sql_detail FROM FVM_MONITORING m\n" +
//                "left outer join FVM_MONITOR_RESULT mr\n" +
//                "on m.id=mr.id\n" +
//                "and mr.is_active='1'";

        String sql = "SELECT m.ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT" +
                ", ERROR_SCHWELLWERT,mr.result as Aktueller_Wert, 100 / Error_schwellwert * case when mr.result>=Error_schwellwert then Error_Schwellwert else mr.result end  / 100 as Error_Prozent" +
                ", Zeitpunkt, m.is_active, m.sql_detail as sql_detail FROM FVM_MONITORING m\n" +
                "left outer join FVM_MONITOR_RESULT mr\n" +
                "on m.id=mr.id\n" +
                "and mr.is_active='1'";

        System.out.println("Abfrage EKP.FVM_Monitoring (CockpitView.java): ");
        System.out.println(sql);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        try {

            jdbcTemplate.setDataSource(ds);

            monitore = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(fvm_monitoring.class));



            System.out.println("FVM_Monitoring eingelesen");

        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            System.out.println("Exception: " + e.getMessage());
        }

        return monitore;
    }


    private List<fvm_monitoring> getHistMonitoring(Integer id) {


        String sql = "select ID,ZEITPUNKT,Result as Aktueller_Wert from FVM_MONITOR_RESULT where id= " + id + " order by Zeitpunkt desc";


        System.out.println("Abfrage EKP.FVM_Monitoring Historie (CockpitView.java): ");
        System.out.println(sql);

//        DriverManagerDataSource ds = new DriverManagerDataSource();
//        Configuration conf;
//        conf = comboBox.getValue();
//
//        ds.setUrl(conf.getDb_Url());
//        ds.setUsername(conf.getUserName());
//        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        try {

       //     jdbcTemplate.setDataSource(ds);
            jdbcTemplate = cockpitService.getJdbcTemplateWithDBConnetion(comboBox.getValue());

            monitore = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(fvm_monitoring.class));



            System.out.println("FVM_Monitoring eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        } finally {
            cockpitService.connectionClose(jdbcTemplate);
        }

        return monitore;
    }


    @Override
    protected void onDetach(DetachEvent event) {
        super.onDetach(event);
        // Stoppe den Timer, wenn das UI geschlossen wird
        stopCountdown();
    }


    private static class ColumnToggleContextMenu extends ContextMenu {
        public ColumnToggleContextMenu(Component target) {
            super(target);
            setOpenOnClick(true);
        }

        void addColumnToggleItem(String label, Grid.Column<fvm_monitoring> column) {
            MenuItem menuItem = this.addItem(label, e -> {
                column.setVisible(e.getSource().isChecked());
            });
            menuItem.setCheckable(true);
            menuItem.setChecked(column.isVisible());
        }
    }
}
