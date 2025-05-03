package com.example.roi.model;

public class RoiChartDataPoint {

    private int year;
    private double cumulativeSavings;
    private String currency = "GBP";

    public RoiChartDataPoint(int year, double cumulativeSavings) {
        this.year = year;
        this.cumulativeSavings = cumulativeSavings;
    }

    // Getters
    public int getYear() {
        return year;
    }

    public double getCumulativeSavings() {
        return cumulativeSavings;
    }

    public String getCurrency() {
        return currency;
    }
}
