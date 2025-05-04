package com.example.roi.model;

public class Tariff {

    private String name;
    private double peakRate;
    private double offpeakRate;
    private double exportRate;
    private boolean evRequired = false;

    public Tariff() {
        // Default constructor
    }

    public Tariff(String name, double peakRate, double offpeakRate, double exportRate) {
        this.name = name;
        this.peakRate = peakRate;
        this.offpeakRate = offpeakRate;
        this.exportRate = exportRate;
    }

    public Tariff(String name, double peakRate, double offpeakRate, double exportRate, boolean evRequired) {
        this.name = name;
        this.peakRate = peakRate;
        this.offpeakRate = offpeakRate;
        this.exportRate = exportRate;
        this.evRequired = evRequired;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPeakRate() {
        return peakRate;
    }

    public void setPeakRate(double peakRate) {
        this.peakRate = peakRate;
    }

    public double getOffpeakRate() {
        return offpeakRate;
    }

    public void setOffpeakRate(double offpeakRate) {
        this.offpeakRate = offpeakRate;
    }

    public double getExportRate() {
        return exportRate;
    }

    public void setExportRate(double exportRate) {
        this.exportRate = exportRate;
    }

    /**
     * Returns true if this tariff requires an EV (electric vehicle).
     */
    public boolean isEvRequired() {
        return evRequired;
    }

    /**
     * Sets whether this tariff requires an EV (electric vehicle).
     */
    public void setEvRequired(boolean evRequired) {
        this.evRequired = evRequired;
    }
}
