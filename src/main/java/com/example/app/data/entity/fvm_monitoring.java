package com.example.app.data.entity;

import jakarta.persistence.*;

import java.sql.Date;

@Entity
@Table(name ="fvm_monitoring", schema="ekp")
public class fvm_monitoring {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates ID via trigger in Oracle
    private Integer ID;
    private Integer Pid;
    private String SQL;
    private String SQL_Detail;
    private String Titel;

    private String Beschreibung;
    private String Handlungs_INFO;
    private Integer Check_Intervall;
    private Integer Warning_Schwellwert;
    private Integer Error_Schwellwert;
    private Double Error_Prozent;
    private String IS_ACTIVE;
    private String bereich;
    private Date Zeitpunkt ;
    private Integer retentionTime;
    private String type;
    private String shell_Server;
    private String shell_Command;
    public String getIS_ACTIVE() {
        return IS_ACTIVE;
    }

    public void setIS_ACTIVE(String IS_ACTIVEe) {
        this.IS_ACTIVE = IS_ACTIVEe;
    }


    public Double getError_Prozent() {
        if (Error_Prozent == null){
            return 0.0;
        }

        return Error_Prozent;
    }

    public void setError_Prozent(Double error_Prozent) {
        Error_Prozent = error_Prozent;
    }


    public Date getZeitpunkt() {
        return Zeitpunkt;
    }

    public void setZeitpunkt(Date zeitpunkt) {
        Zeitpunkt = zeitpunkt;
    }

    public String getTitel() {
        return Titel;
    }

    public void setTitel(String titel) {
        Titel = titel;
    }
    public Integer getAktueller_Wert() {
       // return Aktueller_Wert;
        return (Aktueller_Wert != null) ? Aktueller_Wert : 0;
    }

    public void setAktueller_Wert(Integer aktueller_Wert) {
        Aktueller_Wert = aktueller_Wert;
    }

    private Integer Aktueller_Wert;
    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }
    public Integer getPid() {
        return Pid;
    }

    public void setPid(Integer pid) {
        this.Pid = pid;
    }


    public String getSQL() {
        return SQL;
    }

    public void setSQL(String SQL) {
        this.SQL = SQL;
    }

    public String getBeschreibung() {
        return Beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        Beschreibung = beschreibung;
    }

    public String getHandlungs_INFO() {
        return Handlungs_INFO;
    }

    public void setHandlungs_INFO(String handlungs_INFO) {
        Handlungs_INFO = handlungs_INFO;
    }

    public Integer getCheck_Intervall() {
        return Check_Intervall;
    }

    public void setCheck_Intervall(Integer check_Intervall) {
        Check_Intervall = check_Intervall;
    }

    public Integer getWarning_Schwellwert() {
        return Warning_Schwellwert;
    }

    public void setWarning_Schwellwert(Integer warning_Schwellwert) {
        Warning_Schwellwert = warning_Schwellwert;
    }

    public Integer getError_Schwellwert() {
        return Error_Schwellwert;
    }

    public void setError_Schwellwert(Integer error_Schwellwert) {
        Error_Schwellwert = error_Schwellwert;
    }

    public String getSQL_Detail() {
        return SQL_Detail;
    }

    public void setSQL_Detail(String SQL_Detail) {
        this.SQL_Detail = SQL_Detail;
    }

    public int getRetentionTime() {
        if(retentionTime == null) {
            return 0;
        }
        return retentionTime;
    }

    public void setRetentionTime(Integer retentionTime) {
        this.retentionTime = retentionTime;
        if(retentionTime == null) {
            this.retentionTime = 0;
        }
    }

    public String getBereich() {
        if(bereich == null) {
            return bereich = "";
        }
        return bereich;
    }

    public void setBereich(String bereich) {
        this.bereich = bereich;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        if("SQL".equals(type)) {
            return type = "SQL-Abfrage";
        } else if("SHELL".equals(type)) {
            return type = "Shell-Abfrage";
        }
        return type;
    }

    public void setShellServer(String shell_Server) {
        this.shell_Server = shell_Server;
    }

    public String getShellServer() {
        return shell_Server;
    }

    public void setShellCommand(String shell_Command) {
        this.shell_Command = shell_Command;
    }

    public String getShellCommand() {
        return shell_Command;
    }


}
