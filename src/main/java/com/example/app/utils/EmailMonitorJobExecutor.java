package com.example.app.utils;

import com.example.app.data.entity.*;
import com.example.app.service.CockpitService;
import com.example.app.service.EmailService;
import com.example.app.views.CockpitView;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class EmailMonitorJobExecutor implements Job {


    @Autowired
    private JdbcTemplate jdbcTemplate;
    private EmailService emailService;
    private CockpitService cockpitService;
    private String startType;
    private Configuration configuration;

    private static final Logger logger = LoggerFactory.getLogger(CockpitView.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        startType = context.getMergedJobDataMap().getString("startType");

        cockpitService = SpringContextHolder.getBean(CockpitService.class);
        emailService = SpringContextHolder.getBean(EmailService.class);


        String jobDefinitionString = context.getMergedJobDataMap().getString("configuration");

        try {
            configuration = JobDefinitionUtils.deserializeJobConfDefinition(jobDefinitionString);
            executeJob(configuration);
        } catch (JsonProcessingException e) {
            throw new JobExecutionException("Error deserializing job definition", e);
        }

    }

    private void executeJob(Configuration configuration) {
        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(configuration);

        if (monitorAlerting == null || monitorAlerting.getBgCron() == null) {
            logger.info("EMail-Check: Exit because no configuration or interval is set");
            return; // Exit if no configuration or interval is set
        }

        // Update the LAST_ALERT_CHECKTIME column with the current datetime
        cockpitService.updateLastAlertCheckTimeInDatabase(monitorAlerting, configuration);

        // Check the last alert time to ensure 60 minutes have passed
        LocalDateTime lastAlertTimeFromDB = cockpitService.fetchEmailConfiguration(configuration).getLastAlertTime();
        if (lastAlertTimeFromDB != null && lastAlertTimeFromDB.plusMinutes(60).isAfter(LocalDateTime.now())) {
           // System.out.println("60 minutes have not passed since the last alert. Skipping alert.");
            logger.info("EMail-Check: 60 minutes have not passed since the last alert. Skipping alert.");
            return;
        }

        // Check all monitoring entries
        List<fvm_monitoring> monitorings = cockpitService.getMonitoring(configuration);
        List<fvm_monitoring> exceededEntries = new ArrayList<>(); // List to store entries exceeding the threshold

        // Check each monitoring entry
        for (fvm_monitoring monitoring : monitorings) {
            if (!monitoring.getIS_ACTIVE().equals("1") || monitoring.getPid() == 0) {
                //System.out.println(monitoring.getTitel() + "------------skip-----------" + monitoring.getIS_ACTIVE());
                logger.info("EMail-Check: skip CheckID " + monitoring.getID() + "(isActive= " + monitoring.getIS_ACTIVE() + ")");
                continue; // Skip non-active entries
            }

            if (shouldSendAlert(monitoring)) {
                exceededEntries.add(monitoring); // Add entry to the list if it exceeds the threshold
            }
        }

        // If any entry exceeds the threshold, send an email with an Excel attachment
        if (!exceededEntries.isEmpty()) {
            logger.info("EMail-Check: Count entries exceeded the threshold:" + exceededEntries.size());
            // Generate the Excel file with exceeded entries
            ByteArrayResource xlsxAttachment = generateExcelAttachment(exceededEntries);

            // Send the email with the attachment
            sendAlertEmail(monitorAlerting, xlsxAttachment);
            LocalDateTime lastAlertTime = LocalDateTime.now(); // Update last alert time
            monitorAlerting.setLastAlertTime(lastAlertTime);
            cockpitService.updateLastAlertTimeInDatabase(monitorAlerting, configuration);
        } else {
            logger.info("EMail-Check: No entries exceeded the threshold.");
        }

    }

    private boolean shouldSendAlert(fvm_monitoring monitoring) {
        return monitoring.getAktueller_Wert() >= monitoring.getError_Schwellwert();
    }

    private ByteArrayResource generateExcelAttachment(List<fvm_monitoring> exceededEntries) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Exceeded Threshold Entries");

            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Title");
            headerRow.createCell(2).setCellValue("Aktuell");
            headerRow.createCell(3).setCellValue("Error Schwellwert");

            // Populate rows with data
            int rowIndex = 1;
            for (fvm_monitoring entry : exceededEntries) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(entry.getID());
                row.createCell(1).setCellValue(entry.getTitel());
                row.createCell(2).setCellValue(entry.getAktueller_Wert());
                row.createCell(3).setCellValue(entry.getError_Schwellwert());
            }

            // Write the workbook to a byte array output stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

//            Path projectDir = Paths.get("").toAbsolutePath();
//
//            String fileName = "ExceedEntries.xlsx";
//            // Define the file path relative to the project root directory
//            Path filePath = projectDir.resolve(fileName);
//
//            // Save the workbook to the file
//            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
//                workbook.write(fileOut);
//                System.out.println("File saved to project directory: " + filePath.toAbsolutePath());
//            } catch (IOException e) {
//                e.printStackTrace();
//                throw new IOException("Failed to save Excel file to project directory.", e);
//            }
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate Excel file.", e);
        }
    }

//    private boolean shouldSendEmail(MonitorAlerting monitorAlerting) {
//        // Calculate the next valid email sending time
//        LocalDateTime nextValidTime = monitorAlerting.getLastAlertTime().plusMinutes(monitorAlerting.getIntervall());
//        return LocalDateTime.now().isAfter(nextValidTime);
//    }

    private void sendAlertEmail(MonitorAlerting config, ByteArrayResource resource) {
        try {
            String fileName = "ExceedEntries.xlsx";
            emailService.sendAttachMessage(config.getMailEmpfaenger(), config.getMailCCEmpfaenger(), config.getMailBetreff(), config.getMailText(), fileName, resource);
         //   emailService.sendAttachMessage(config.getMailEmpfaenger(), config.getMailCCEmpfaenger(), config.getMailBetreff(), config.getMailText());

            logger.info("Email send to " + config.getMailEmpfaenger());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

