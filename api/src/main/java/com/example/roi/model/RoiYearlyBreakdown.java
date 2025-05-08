package com.example.roi.model;

public class RoiYearlyBreakdown {
    private int year;
    private double usableBatteryMaxCapacity;
    private double degradationFactor;
    private double shiftable;
    private double batterySavings;
    private double solarUsed;
    private double solarExport;
    private double solarSavingsSelfUse;
    private double solarSavingsExport;
    private double yearlyTotalSavings;
    private double cumulativeSavings;

    public RoiYearlyBreakdown(int year, double usableBatteryMaxCapacity, double degradationFactor, double shiftable,
                              double batterySavings, double solarUsed, double solarExport, double solarSavingsSelfUse,
                              double solarSavingsExport, double yearlyTotalSavings, double cumulativeSavings) {
        this.year = year;
        this.usableBatteryMaxCapacity = usableBatteryMaxCapacity;
        this.degradationFactor = degradationFactor;
        this.shiftable = shiftable;
        this.batterySavings = batterySavings;
        this.solarUsed = solarUsed;
        this.solarExport = solarExport;
        this.solarSavingsSelfUse = solarSavingsSelfUse;
        this.solarSavingsExport = solarSavingsExport;
        this.yearlyTotalSavings = yearlyTotalSavings;
        this.cumulativeSavings = cumulativeSavings;
    }

    public int getYear() { return year; }
    public double getUsableBatteryMaxCapacity() { return usableBatteryMaxCapacity; }
    public double getDegradationFactor() { return degradationFactor; }
    public double getShiftable() { return shiftable; }
    public double getBatterySavings() { return batterySavings; }
    public double getSolarUsed() { return solarUsed; }
    public double getSolarExport() { return solarExport; }
    public double getSolarSavingsSelfUse() { return solarSavingsSelfUse; }
    public double getSolarSavingsExport() { return solarSavingsExport; }
    public double getYearlyTotalSavings() { return yearlyTotalSavings; }
    public double getCumulativeSavings() { return cumulativeSavings; }
} 