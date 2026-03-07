package com.example.app.views;

import com.example.app.data.entity.Durchsatz;
import com.example.app.data.entity.Configuration;
import com.example.app.data.service.ConfigurationService;
import com.example.app.data.service.CrmService;
import com.example.app.service.BackendService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.Async;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard")
@Menu(title = "Dashboard", order = 1, icon = "vaadin:chart")
@RolesAllowed("ADMIN")
public class DashboardView extends VerticalLayout {

    private final ConfigurationService configurationService;

    private JdbcTemplate jdbcTemplate = new JdbcTemplate();

    private final Span currentPrice = new Span();
    private final Span clockLabel = new Span("...");

    private ComboBox<Configuration> comboBox;

    private ListSeries series = new ListSeries("Speed", 139);
    private Integer refreshIntervall = 10000;

    private ScheduledExecutorService executor;
    private Chart chart1 = new Chart();
    private Chart line1;

    private final Timer timer = new Timer();
    private Integer anzahlTimer = 0;

    private static final Logger logger = LoggerFactory.getLogger(DashboardView.class);

    private UI ui;

    public DashboardView(CrmService service,
                         BackendService bkService,
                         ConfigurationService configurationService) throws ParseException {

        this.configurationService = configurationService;

        comboBox = new ComboBox<>("Verbindung");
        List<Configuration> configList = configurationService.findMessageConfigurations();
        if (configList != null && !configList.isEmpty()) {
            comboBox.setItems(configList);
            comboBox.setItemLabelGenerator(Configuration::getName);
        }
        comboBox.setPlaceholder("auswählen");

        ComboBox<String> refreshCB = new ComboBox<>("Refresh Intervall");
        refreshCB.setItems("5", "10", "15", "30");
        refreshCB.setHelperText("Intervall in Sekunden");

        refreshCB.addValueChangeListener(value -> {
            if (value.getValue() == null) {
                return;
            }

            refreshIntervall = Integer.parseInt(value.getValue()) * 1000;
            logger.info("Refresh Intervall: {}", refreshIntervall);

            startTimer();

            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }

            AtomicReference<Integer> rest = new AtomicReference<>(refreshIntervall);

            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(() -> {
                rest.set(rest.get() - 1000);
                if (rest.get() <= 0) {
                    rest.set(refreshIntervall);
                }

                String currentTime = getCurrentTimeAsString() + " Anzahl Timer Objekte: " + anzahlTimer;

                getUI().ifPresent(currentUi -> currentUi.access(() -> clockLabel.setText(currentTime)));
            }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);
        });

        HorizontalLayout header = new HorizontalLayout();
        header.add(comboBox, refreshCB);

        add(header);
        add(clockLabel);

        IFrame iframe = new IFrame();
        iframe.setSrc("https://www.dbuss.de");
        iframe.setWidthFull();
        iframe.setHeight("400px");

        Anchor a = new Anchor("https://www.dbuss.de", "DBUSS");
        // add(iframe, a);

        chart1 = buildChart(99);
        add(chart1);

        line1 = new Chart();
        final com.vaadin.flow.component.charts.model.Configuration lineConfiguration = line1.getConfiguration();
        lineConfiguration.getChart().setType(ChartType.SPLINE);
        lineConfiguration.getTitle().setText("Tagseverlauf");

        XAxis xAxis = lineConfiguration.getxAxis();
        xAxis.setType(AxisType.DATETIME);
        xAxis.setTickPixelInterval(150);

        YAxis yAxis = lineConfiguration.getyAxis();
        yAxis.setTitle(new AxisTitle("Anzahl"));

        lineConfiguration.getTooltip().setEnabled(false);
        lineConfiguration.getLegend().setEnabled(false);

        DataSeries lineSeries = getDurchsatz();
        if (lineSeries != null) {
            lineConfiguration.setSeries(lineSeries);
            lineSeries.setConfiguration(lineConfiguration);
        }

        add(line1);

        final TextField tf = new TextField("Enter a new value");
        add(tf);

        Button update = new Button("Update", e -> {
            if (tf.getValue() == null || tf.getValue().isBlank()) {
                return;
            }

            Integer newValue = Integer.valueOf(tf.getValue());
            com.vaadin.flow.component.charts.model.Configuration conf = chart1.getConfiguration();

            series = new ListSeries("Speed", newValue);
            conf.setSeries(series);
            chart1.drawChart();
        });
        add(update);

        final Chart chart = new Chart(ChartType.COLUMN);
        chart.setId("chart");

        final com.vaadin.flow.component.charts.model.Configuration conf = chart.getConfiguration();
        conf.setTitle("Nachrichtendurchsatz");
        conf.setSubTitle("Quelle eKP / EGVP-W");
        conf.getLegend().setEnabled(false);

        XAxis x = new XAxis();
        x.setType(AxisType.CATEGORY);
        conf.addxAxis(x);

        YAxis y = new YAxis();
        y.setTitle("Anzahl Nachrichten");
        conf.addyAxis(y);

        PlotOptionsColumn column = new PlotOptionsColumn();
        column.setCursor(Cursor.POINTER);
        column.setDataLabels(new DataLabels(true));
        conf.setPlotOptions(column);

        DataSeries regionsSeries = new DataSeries();
        regionsSeries.setName("Gesamt");

        PlotOptionsColumn plotOptionsColumn = new PlotOptionsColumn();
        plotOptionsColumn.setColorByPoint(true);
        regionsSeries.setPlotOptions(plotOptionsColumn);

        regionsSeries.add(new DataSeriesItem("Gesamt", 120));
        conf.addSeries(regionsSeries);

        add(chart);
    }

    private String getCurrentTimeAsString() {
        LocalTime currentTime = LocalTime.now();
        return String.format("%02d:%02d:%02d",
                currentTime.getHour(),
                currentTime.getMinute(),
                currentTime.getSecond());
    }

    private DataSeries getDurchsatz() throws ParseException {
        Configuration conf = comboBox.getValue();
        DataSeries series = new DataSeries();

        if (conf == null) {
            return null;
        }

        List<Durchsatz> dl = new ArrayList<>();

        String sql = """
                select trunc(Eingangsdatumserver,'HH') Zeitpunkt, count(*) as Anzahl
                from ekp.metadaten
                where Eingangsdatumserver is not null
                  and Eingangsdatumserver > sysdate -2
                group by trunc(Eingangsdatumserver,'HH')
                order by 1 desc
                """;

        if (jdbcTemplate == null) {
            logger.warn("jdbcTemplate in getDurchsatz ist NULL, liefere Default-Werte");
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date dd = sdf.parse("08:30:54");
            series.add(new DataSeriesItem(dd.toInstant(), 50));
            return series;
        }

        try {
            JdbcTemplate localJdbcTemplate = getJdbcTemplateWithDBConnection(conf);

            dl = localJdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper<>(Durchsatz.class)
            );

            logger.info("Durchsatz eingelesen");
        } catch (Exception e) {
            logger.error("Exception in getDurchsatz: {}", e.getMessage(), e);
        }

        for (Durchsatz obj : dl) {
            Date zeit = obj.getZeitpunkt();
            if (zeit != null) {
                series.add(new DataSeriesItem(zeit.toInstant(), obj.getAnzahl()));
            }
        }

        return series;
    }

    public JdbcTemplate getJdbcTemplateWithDBConnection(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());

        String plainPassword = configurationService.getPlainPasswordAndMigrateIfNeeded(conf);
        ds.setPassword(plainPassword);

        JdbcTemplate localJdbcTemplate = new JdbcTemplate(ds);
        this.jdbcTemplate = localJdbcTemplate;
        return localJdbcTemplate;
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null || jdbcTemplate.getDataSource() == null) {
            return;
        }

        try {
            jdbcTemplate.getDataSource().getConnection().close();
        } catch (SQLException e) {
            logger.error("Fehler beim Schließen der Connection: {}", e.getMessage(), e);
        }
    }

    private Chart buildChart(Integer wert) {
        Chart chart = new Chart();
        final com.vaadin.flow.component.charts.model.Configuration configuration = chart.getConfiguration();
        configuration.getChart().setType(ChartType.GAUGE);
        configuration.setTitle("aktueller Durchsatz");
        configuration.getChart().setWidth(600);

        Pane pane = configuration.getPane();
        pane.setStartAngle(-150);
        pane.setEndAngle(150);

        YAxis yAxis = new YAxis();
        yAxis.setTitle("Nachrichten/h");
        yAxis.setMin(0);
        yAxis.setMax(1600);
        yAxis.setTickLength(10);
        yAxis.setTickPixelInterval(30);
        yAxis.setTickPosition(TickPosition.INSIDE);
        yAxis.setMinorTickLength(10);
        yAxis.setMinorTickInterval("auto");
        yAxis.setMinorTickPosition(TickPosition.INSIDE);

        Labels labels = new Labels();
        labels.setStep(2);
        labels.setRotation("auto");
        yAxis.setLabels(labels);

        PlotBand[] bands = new PlotBand[3];
        bands[0] = new PlotBand();
        bands[0].setFrom(0);
        bands[0].setTo(120);
        bands[0].setClassName("band-0");

        bands[1] = new PlotBand();
        bands[1].setFrom(120);
        bands[1].setTo(160);
        bands[1].setClassName("band-1");

        bands[2] = new PlotBand();
        bands[2].setFrom(160);
        bands[2].setTo(200);
        bands[2].setClassName("band-2");

        yAxis.setPlotBands(bands);
        configuration.addyAxis(yAxis);

        series = new ListSeries("Speed", wert);

        PlotOptionsGauge plotOptionsGauge = new PlotOptionsGauge();
        SeriesTooltip tooltip = new SeriesTooltip();
        tooltip.setValueSuffix(" N/h");
        plotOptionsGauge.setTooltip(tooltip);
        series.setPlotOptions(plotOptionsGauge);

        configuration.addSeries(series);

        return chart;
    }

    void refresh(List<Durchsatz> res) {
        if (res == null || res.isEmpty()) {
            return;
        }

        Integer anzahl = res.stream().findFirst().map(Durchsatz::getAnzahl).orElse(0);

        com.vaadin.flow.component.charts.model.Configuration conf = chart1.getConfiguration();
        series = new ListSeries("Speed", anzahl);
        conf.setSeries(series);
        chart1.drawChart();

        conf = line1.getConfiguration();

        try {
            DataSeries dataSeries = getDurchsatz();
            if (dataSeries != null) {
                conf.setSeries(dataSeries);
                line1.drawChart();
            }
        } catch (Exception e) {
            logger.error("Ermittlung der bisherigen Serie nicht erfolgreich: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ui = attachEvent.getUI();
    }

    private void startTimer() {
        timer.purge();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Configuration conf = comboBox.getValue();
                if (conf == null || ui == null) {
                    return;
                }

                loadDurchsatzAsync(conf)
                        .thenAccept(result -> ui.access(() -> {
                            if (result == null || result.isEmpty()) {
                                return;
                            }

                            Integer anzahl = result.stream().findFirst().map(Durchsatz::getAnzahl).orElse(0);

                            com.vaadin.flow.component.charts.model.Configuration configuration = chart1.getConfiguration();
                            series = new ListSeries("Speed", anzahl);
                            configuration.setSeries(series);

                            currentPrice.setText("Wert: " + anzahl);

                            refresh(result);
                        }))
                        .exceptionally(err -> {
                            ui.access(() -> Notification.show("Fehler: " + err.getMessage()));
                            return null;
                        });
            }
        }, 0, refreshIntervall);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        logger.debug("In onDetach Methode");
        timer.cancel();
        timer.purge();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        super.onDetach(detachEvent);
    }

    @Async
    public CompletableFuture<List<Durchsatz>> loadDurchsatzAsync(Configuration conf) {
        List<Durchsatz> dl = new ArrayList<>();

        String sql = """
                select trunc(Eingangsdatumserver,'HH') Zeit, count(*) as Anzahl
                from ekp.metadaten
                where Eingangsdatumserver is not null
                  and Eingangsdatumserver > sysdate -2
                group by trunc(Eingangsdatumserver,'HH')
                order by 1 desc
                """;

        try {
            JdbcTemplate localJdbcTemplate = getJdbcTemplateWithDBConnection(conf);

            dl = localJdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper<>(Durchsatz.class)
            );

            logger.info("Durchsatz eingelesen");
        } catch (Exception e) {
            logger.error("Exception in loadDurchsatzAsync: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(dl);
    }
}