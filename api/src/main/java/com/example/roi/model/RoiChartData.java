package com.example.roi.model;

import java.util.List;

public class RoiChartData {

    private List<RoiChartDataPoint> dataPoints;
    private Integer breakEvenYear; // Use Integer to allow null

    public RoiChartData(List<RoiChartDataPoint> dataPoints, Integer breakEvenYear) {
        this.dataPoints = dataPoints;
        this.breakEvenYear = breakEvenYear;
    }

    // Getters
    public List<RoiChartDataPoint> getDataPoints() {
        return dataPoints;
    }

    public Integer getBreakEvenYear() {
        return breakEvenYear;
    }
}
