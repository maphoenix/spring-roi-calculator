package com.example.roi.model;

public class RoiPercentage {

    private double percentage;
    private int periodYears;

    public RoiPercentage(double percentage, int periodYears) {
        this.percentage = percentage;
        this.periodYears = periodYears;
    }

    // Getters
    public double getPercentage() {
        return percentage;
    }

    public int getPeriodYears() {
        return periodYears;
    }
}
