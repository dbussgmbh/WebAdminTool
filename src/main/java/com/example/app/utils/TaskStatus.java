package com.example.app.utils;

public class TaskStatus {
    private boolean active;
    private String Logfile;

    public String getLogfile() {
        return Logfile;
    }

    public void setLogfile(String logfile) {
        Logfile = logfile;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
