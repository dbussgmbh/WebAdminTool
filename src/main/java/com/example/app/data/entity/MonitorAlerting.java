package com.example.app.data.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MonitorAlerting {

    private Long id;
    private String mailEmpfaenger;
    private String mailCCEmpfaenger;
    private String mailBetreff;
    private String mailText;
  //  private Integer intervall;
    private String bgCron;
    private String mbWatchdogCron;
    private Integer isActive;
    private Integer isBackJobActive;
    private Integer isMBWatchdogActive;
    private LocalDateTime lastAlertTime;
    private LocalDateTime lastALertCheckTime;
    private int retentionTime;
    private int maxParallelCheck;
    private String watchdogMailEmpfaenger;
    private String watchdogMailCCEmpfaenger;
    private String watchdogMailBetreff;
    private String watchdogMailText;
    private Integer simulation;
    // Default constructor
    public MonitorAlerting() {}

    // Parameterized constructor
//    public MonitorAlerting(Long id, String mailEmpfaenger, String mailCCEmpfaenger,
//                           String mailBetreff, String mailText, Integer intervall) {
//        this.id = id;
//        this.mailEmpfaenger = mailEmpfaenger;
//        this.mailCCEmpfaenger = mailCCEmpfaenger;
//        this.mailBetreff = mailBetreff;
//        this.mailText = mailText;
//        this.intervall = intervall;
//    }

    @Override
    public String toString() {
        return "MonitorAlerting{" +
                "id=" + id +
                ", mailEmpfaenger='" + mailEmpfaenger + '\'' +
                ", mailCCEmpfaenger='" + mailCCEmpfaenger + '\'' +
                ", mailBetreff='" + mailBetreff + '\'' +
                ", mailText='" + mailText + '\'' +
                ", watchdogMailEmpfaenger='" + watchdogMailEmpfaenger + '\'' +
                ", watchdogMailCCEmpfaenger='" + watchdogMailCCEmpfaenger + '\'' +
                ", watchdogMailBetreff='" + watchdogMailBetreff + '\'' +
                ", watchdogMailText='" + watchdogMailText + '\'' +
                ", mbWatchdogCron='" + mbWatchdogCron + '\'' +
                ", simulation='" + simulation + '\'' +
                ", bg_cron=" + bgCron +
             //   ", intervall=" + intervall +
                '}';
    }
}
