package com.example.app.data.entity;

import java.util.Date;

public class Durchsatz
{
    Date Zeitpunkt;
    String Art;
    Integer Anzahl;

    public Date getZeitpunkt() {
        return Zeitpunkt;
    }

    public void setZeitpunkt(Date zeitpunkt) {
        Zeitpunkt = zeitpunkt;
    }

    public String getArt() {
        return Art;
    }

    public void setArt(String art) {
        Art = art;
    }

    public Integer getAnzahl() {
        return Anzahl;
    }

    public void setAnzahl(Integer anzahl) {
        Anzahl = anzahl;
    }
}
