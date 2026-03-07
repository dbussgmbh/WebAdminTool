package com.example.app.views;

import com.example.app.data.entity.Durchsatz;
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
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.Async;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;


@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard")
@Menu(title = "Dashboard", order = 1, icon = "vaadin:chart")
@RolesAllowed("ADMIN")
public class DashboardView extends VerticalLayout {

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Span currentPrice = new Span();

    private ComboBox<com.example.app.data.entity.Configuration> comboBox;


    ListSeries series = new ListSeries("Speed", 139);
    Integer refreshIntervall=10000;
    Span clockLabel = new Span("...");
    ScheduledExecutorService executor;
    Chart chart1 = new Chart();
    Chart line1;

    Timer timer= new Timer();
    Integer AnzahlTimer=0;
    private static final Logger logger = LoggerFactory.getLogger(DashboardView.class);

    UI ui;


    private String getCurrentTimeAsString() {
        LocalTime currentTime = LocalTime.now();
        return String.format("%02d:%02d:%02d", currentTime.getHour(), currentTime.getMinute(), currentTime.getSecond());

    }

    public DashboardView(CrmService service, BackendService bk_service, ConfigurationService conf_service) throws ParseException {
        //    this.service = service;
        //    this.bk_service=bk_service;

        //    this.service = service;

        comboBox = new ComboBox<>("Verbindung");
        List<com.example.app.data.entity.Configuration> configList = conf_service.findMessageConfigurations();
        if (configList != null && !configList.isEmpty()) {
            comboBox.setItems(configList);
            comboBox.setItemLabelGenerator(com.example.app.data.entity.Configuration::getName);
        }

        //    comboBox.setValue(service.findAllConfigurations().stream().findFirst().get());
        comboBox.setPlaceholder("auswählen");


        //add(new H1("FVM-Status Dashboard"));


        //  Paragraph paragraph= new Paragraph("Hier ist die Anzeige von aktuellen Metriken aus der DB geplant");
        //  paragraph.setMaxHeight("400px");
        //  add(paragraph,comboBox);


        ComboBox refreshCB = new ComboBox<>("Refresh Intervall");
        refreshCB.setItems("5", "10", "15", "30");
        refreshCB.setHelperText("Intervall in Minuten");
        refreshCB.addValueChangeListener(value -> {

            System.out.println("Refresh Intervall: " + value.getValue());
            refreshIntervall= Integer.parseInt(value.getValue().toString()) * 1000;

            System.out.println("Refresh Intervall: " + refreshIntervall.toString());



            /*    try{
                    timer.cancel();
                    timer.purge();
                }
                catch (Exception e){
                    System.out.println("Timer läuft aktuell nicht");
                    //System.out.println(e.getMessage());

                }*/



            start_timer();

            if (executor!= null &&  !executor.isShutdown())
            {
                executor.shutdown();
            }



            AtomicReference<Integer> rest= new AtomicReference<>(refreshIntervall);

            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(() -> {
                rest.set(rest.get() - 1000);
                if (rest.get() <= 0)
                {
                    rest.set(refreshIntervall);
                }
                //String currentTime = getCurrentTimeAsString() + " nächster Refresh in " + rest +" Sekunden" ;
                String currentTime = getCurrentTimeAsString() + " Anzahl Timer Objekte: " + AnzahlTimer;


                getUI().ifPresent(ui -> ui.access(() -> {
                    clockLabel.setText(currentTime);
                }));
            }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);


        });

        HorizontalLayout header = new HorizontalLayout();
        //  currentPrice.setText("Aktueller Wert: " + series.toString());


        header.add(comboBox,refreshCB);

        add(header);

        add(clockLabel);




        //Iframe iframe = new Iframe();

        IFrame iframe = new IFrame();
        iframe.setSrc("https:\\www.dbuss.de");
        iframe.setWidthFull();
        iframe.setHeight("400px");

        // add(iframe);

        Anchor a = new Anchor("https:\\www.dbuss.de","DBUSS");
        //add(a);

  /*      Chart chart2 = new Chart();
        Configuration configuration2 = chart2.getConfiguration();

        configuration2.getChart().setType(ChartType.LINE);

        configuration2.getxAxis().setCategories("Jan", "Feb", "Mar", "Apr");

        DataSeries ds = new DataSeries();
        ds.setData(7.0, 6.9, 9.5, 14.5);

        DataLabels callout = new DataLabels(true);
        callout.setShape(Shape.CALLOUT);
        callout.setY(-12);
        ds.get(1).setDataLabels(callout);
        ds.get(2).setDataLabels(callout);
        configuration2.addSeries(ds);

        chart2.addClassName("first-chart");
        add(chart2);*/







        chart1 = build_chart(99);
        add(chart1);


        line1 = new Chart();
        final Random random = new Random();
        final Configuration configuration = line1.getConfiguration();
        configuration.getChart().setType(ChartType.SPLINE);
        configuration.getTitle().setText("Tagseverlauf");


        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);
        xAxis.setTickPixelInterval(150);

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle(new AxisTitle("Anzahl"));

        configuration.getTooltip().setEnabled(false);
        configuration.getLegend().setEnabled(false);

        DataSeries line_series = new DataSeries();
        line_series.setPlotOptions(new PlotOptionsSpline());
        //  line_series.setName("Random data");
       /* for (int i = -19; i <= 0; i++) {
            line_series.add(new DataSeriesItem(System.currentTimeMillis() + i * 1000, random.nextDouble()));
        }*/

        //   TimeZone berlinTimeZone = TimeZone.getTimeZone("Europe/Berlin");
        TimeZone berlinTimeZone = TimeZone.getTimeZone("GMT+2:00");
        Date now = new Date();
        long nowInBerlin = now.getTime() + berlinTimeZone.getRawOffset();


        line_series=getDurchsatz();

        if(line_series!=null)
        {
            configuration.setSeries(line_series);
            line_series.setConfiguration (configuration);
        }

        add(line1);


        final TextField tf = new TextField("Enter a new value");
        add(tf);


        Button update = new Button("Update", (e) -> {
            Integer newValue = Integer.valueOf(tf.getValue());
            Configuration conf = chart1.getConfiguration();
            //  List<Series> xx = conf.getSeries();

            series = new ListSeries("Speed", newValue);

            conf.setSeries(series);
            chart1.drawChart();
        });
        add(update);



        final Chart chart = new Chart(ChartType.COLUMN);
        chart.setId("chart");

        final Configuration conf = chart.getConfiguration();

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

        DataSeriesItem regionItem = new DataSeriesItem(
                "Gesamt", 120);

        DataSeries countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("Latin America and Carribean Countries");

        DataSeriesItem countryItem = new DataSeriesItem("Costa Rica", 64);
        DataSeries detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Costa Rica");
        String[] categories = new String[] { "Life Expectancy",
                "Well-being (0-10)", "Footprint (gha/capita)" };
        Number[] ys = new Number[] { 79.3, 7.3, 2.5 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Colombia", 59.8);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Colombia");
        ys = new Number[] { 73.7, 6.4, 1.8 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Belize", 59.3);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Belize");
        ys = new Number[] { 76.1, 6.5, 2.1 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("El Salvador", 58.9);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details El Salvador");
        ys = new Number[] { 72.2, 6.7, 2.0 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        regionItem = new DataSeriesItem("Western Nations", 50);

        countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("Western Nations Countries");

        countryItem = new DataSeriesItem("New Zealand", 51.6);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details New Zealand");
        ys = new Number[] { 80.7, 7.2, 4.3 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Norway", 51.4);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Norway");
        ys = new Number[] { 81.1, 7.6, 4.8 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Switzerland", 50.3);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Switzerland");
        ys = new Number[] { 82.3, 7.5, 5.0 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("United Kingdom", 47.9);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details United Kingdom");
        ys = new Number[] { 80.2, 7.0, 4.7 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        regionItem = new DataSeriesItem("Middle East and North Africa", 53);

        countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("Middle East and North Africa Countries");

        countryItem = new DataSeriesItem("Israel", 55.2);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Israel");
        ys = new Number[] { 81.6, 7.4, 4.0 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Algeria", 52.2);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Algeria");
        ys = new Number[] { 73.1, 5.2, 1.6 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Jordan", 51.7);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Jordan");
        ys = new Number[] { 73.4, 5.7, 2.1 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Palestine", 51.2);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Palestine");
        ys = new Number[] { 72.8, 4.8, 1.4 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        regionItem = new DataSeriesItem("Sub-Saharan Africa", 42);

        countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("Sub-Saharan Africa Countries");

        countryItem = new DataSeriesItem("Nigeria", 51.6);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Nigeria");
        ys = new Number[] { 66.7, 4.6, 1.2 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Malawi", 42.5);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Malawi");
        ys = new Number[] { 54.2, 5.1, 0.8 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Ghana", 40.3);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Ghana");
        ys = new Number[] { 64.2, 4.6, 1.7 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Ethiopia", 39.2);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Ethiopia");
        ys = new Number[] { 59.3, 4.4, 1.1 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        regionItem = new DataSeriesItem("South Asia", 53);

        countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("South Asia Countries");

        countryItem = new DataSeriesItem("Bangladesh", 56.3);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Bangladesh");
        ys = new Number[] { 68.9, 5.0, 0.7 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Pakistan", 54.1);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Pakistan");
        ys = new Number[] { 65.4, 5.3, 0.8 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("India", 50.9);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details India");
        ys = new Number[] { 65.4, 5.0, 0.9 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Sri Lanka", 51.2);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Sri Lanka");
        ys = new Number[] { 74.9, 4.2, 1.2 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        regionItem = new DataSeriesItem("East Asia", 55);

        countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("East Asia Countries");

        countryItem = new DataSeriesItem("Vietnam", 60.4);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Vietnam");
        ys = new Number[] { 75.2, 5.8, 1.4 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Indonesia", 55.5);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Indonesia");
        ys = new Number[] { 69.4, 5.5, 1.1 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Thailand", 53.5);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Thailand");
        ys = new Number[] { 74.1, 6.2, 2.4 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Philippines", 52.4);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Philippines");
        ys = new Number[] { 68.7, 4.9, 1.0 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        conf.addSeries(regionsSeries);

        add(chart);





    }


    private DataSeries getDurchsatz() throws ParseException {
        com.example.app.data.entity.Configuration conf; // = new com.example.application.data.entity.Configuration("HH", "Prod", "SYSTEM", "Michael123", "jdbc:oracle:thin:@37.120.189.200:1521:xe" );
        conf = comboBox.getValue();

        DataSeries series = new DataSeries();

        if (conf == null)
        {
            return null;
        }

        List<Durchsatz> dl = new ArrayList<Durchsatz>();

        Durchsatz d = new Durchsatz();


        //      String sql = "select trunc(Eingangsdatumserver,'HH') Zeitpunkt, Art,count(*) as Anzahl from ekp.metadaten where Eingangsdatumserver is not null and art='incoming' and Eingangsdatumserver > sysdate -3 group by trunc(Eingangsdatumserver,'HH') ,art order by 1 desc" ;
        String sql = "select trunc(Eingangsdatumserver,'HH') Zeitpunkt, count(*) as Anzahl from ekp.metadaten where Eingangsdatumserver is not null and Eingangsdatumserver > sysdate -2 group by trunc(Eingangsdatumserver,'HH') order by 1 desc" ;

        System.out.println("Info: Abfrage EKP.Metadaten");

//        DriverManagerDataSource ds = new DriverManagerDataSource();
//
//        ds.setUrl(conf.getDb_Url());
//        ds.setUsername(conf.getUserName());
//        ds.setPassword(com.example.application.data.entity.Configuration.decodePassword(conf.getPassword()));

        if (jdbcTemplate == null)
        {
            System.out.println("Achtung: jdbyTemplate in getDurchsatz ist NULL! Liefer default Werte...");

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date dd = sdf.parse("08:30:54");
            series.add(new DataSeriesItem(dd.toInstant(), 50));

            return series;
        }


        try {



            //     jdbcTemplate.setDataSource(ds);
            getJdbcTemplateWithDBConnetion(comboBox.getValue());

            dl = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Durchsatz.class));


            System.out.println("Durchsatz eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        } finally {
            connectionClose(jdbcTemplate);
        }


        /*

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        series.add(new DataSeriesItem(sdf.parse("08:30:54"), 50));
        series.add(new DataSeriesItem(sdf.parse("09:30:54"), 20));
        series.add(new DataSeriesItem(sdf.parse("10:30:54"), 15));
        series.add(new DataSeriesItem(sdf.parse("11:30:54"), 10));
        series.add(new DataSeriesItem(sdf.parse("12:30:54"), 110));
        series.add(new DataSeriesItem(sdf.parse("13:30:54"), 210));
*/



        for (Durchsatz obj : dl) {
            Date zeit = obj.getZeitpunkt();
            if (zeit != null) {
                series.add(new DataSeriesItem(zeit.toInstant(), obj.getAnzahl()));
            }
        }

        return series;

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

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        Connection connection = null;
        DataSource dataSource = null;
        try {
            jdbcTemplate.getDataSource().getConnection().close();
            //     connection = jdbcTemplate.getDataSource().getConnection();
            //     dataSource = jdbcTemplate.getDataSource();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();

                    // System.out.println("connection closed..."+connection.isClosed() +".....");
                    if (dataSource instanceof HikariDataSource) {
                        ((HikariDataSource) dataSource).close();
                    } else {
                        dataSource.getConnection().close();
                    }

                } catch (SQLException e) {

                    e.printStackTrace();
                }
            }
        }
    }
    private Chart build_chart(Integer wert) {

        Chart chart = new Chart();
        final Configuration configuration = chart.getConfiguration();
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

        series = new ListSeries("Speed", 139);

        PlotOptionsGauge plotOptionsGauge = new PlotOptionsGauge();
        SeriesTooltip tooltip = new SeriesTooltip();
        tooltip.setValueSuffix("N/h");
        plotOptionsGauge.setTooltip(tooltip);
        series.setPlotOptions(plotOptionsGauge);

        configuration.addSeries(series);

        return chart;
    }



    void refresh(List<Durchsatz> res){
        // System.out.println(("Refresh wurde aufgerufen: Übergebener Wert" + res));

        Integer Anzahl = res.stream().findFirst().get().getAnzahl();

        Configuration conf = chart1.getConfiguration();
        series = new ListSeries("Speed", Anzahl);
        conf.setSeries(series);
        chart1.drawChart();

// Ab hier für Line-Chart:


        TimeZone berlinTimeZone = TimeZone.getTimeZone("GMT+2:00");
        Date now = new Date();
        long nowInBerlin = now.getTime() + berlinTimeZone.getRawOffset();
        conf = line1.getConfiguration();

        System.out.println(("Refresh wurde aufgerufen: Werte: " + nowInBerlin  + " => " + Anzahl.toString()));
        //line_series.add(new DataSeriesItem(System.currentTimeMillis() * 1000, 99));

        try{

            //   DataSeries existingDataSeries = (DataSeries) line1.getConfiguration().getSeries().get(0);
            //   existingDataSeries.add(new DataSeriesItem(nowInBerlin, Anzahl));

            DataSeries dataSeries = new DataSeries();
            dataSeries=getDurchsatz();

            conf.setSeries(dataSeries);
            line1.drawChart();


        }
        catch(Exception e)
        {
            System.out.println("Ermittlung der bisherigen Serie nicht erfolgreich! " + e.getMessage());
        }


        // line_series.add(new DataSeriesItem(new Date() , res));

//        line_series = (DataSeries) line_conf.getSeries();
        //      line_series.setName("Random data");
        //    line_series.add(new DataSeriesItem(System.currentTimeMillis() * 1000, res));
        //ss.add(new DataSeriesItem(System.currentTimeMillis() * 1000, res));

        //   line_conf.setSeries(line_series);
        // line_conf.setSeries(line_series);



    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        ui = attachEvent.getUI();

        //String session = VaadinSession.getCurrent().getSession().getId();
        //System.out.println("In onAttache Methode, session=" + session);

        // ui.access(()->currentPrice.setText("huhu"));

        //start_timer();

    }

    private void start_timer() {

        //UI cui = UI.getCurrent();

        timer.purge();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                DriverManagerDataSource ds = new DriverManagerDataSource();
                com.example.app.data.entity.Configuration conf;
                conf = comboBox.getValue();

                if (conf == null)
                {
                    return;
                }


                LoadDurchsatzAsync(conf)
                        .thenAccept(result -> ui.access(() -> {
                            Integer anzahl = result.stream().findFirst().get().getAnzahl();

                            Configuration configuration = chart1.getConfiguration();
                            series = new ListSeries("Speed", anzahl);
                            configuration.setSeries(series);

                            currentPrice.setText("Wert:" + anzahl);

                            refresh(result);
                        }))
                        .exceptionally(err -> {
                            ui.access(() -> Notification.show("BOO: " + err.getMessage()));
                            return null;
                        });


            }

        }, 0, refreshIntervall);

    }


    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // Cancel subscription when the view is detached
        // subscription.dispose();
        logger.debug("In onDetache Methode");
        timer.cancel();
        timer.purge();
        super.onDetach(detachEvent);
    }

    @Async
    public CompletableFuture<List<Durchsatz>> LoadDurchsatzAsync(com.example.app.data.entity.Configuration conf) {

        List<Durchsatz> dl = new ArrayList<Durchsatz>();


        Durchsatz d = new Durchsatz();

        d.setZeitpunkt(new Date());
        d.setArt("incoming");
        d.setAnzahl(250);
        //    dl.add(d);


        //  String sql = "select trunc(Eingangsdatumserver,'HH') Zeit, Art,count(*) as Anzahl from ekp.metadaten where Eingangsdatumserver is not null and Art='incoming' and Eingangsdatumserver > sysdate -3 group by trunc(Eingangsdatumserver,'HH') ,art order by 1 desc" ;
        String sql = "select trunc(Eingangsdatumserver,'HH') Zeit, count(*) as Anzahl from ekp.metadaten where Eingangsdatumserver is not null and Eingangsdatumserver > sysdate -2 group by trunc(Eingangsdatumserver,'HH') order by 1 desc" ;

        System.out.println("Info: Abfrage EKP.Metadaten");

//        DriverManagerDataSource ds = new DriverManagerDataSource();
//
//        ds.setUrl(conf.getDb_Url());
//        ds.setUsername(conf.getUserName());
//        ds.setPassword(com.example.application.data.entity.Configuration.decodePassword(conf.getPassword()));


        try {

//            jdbcTemplate.setDataSource(ds);
            getJdbcTemplateWithDBConnetion(comboBox.getValue());

            dl = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Durchsatz.class));


            System.out.println("Durchsatz eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        } finally {
            connectionClose(jdbcTemplate);
        }

        return CompletableFuture.completedFuture(dl);
    }
}




