package com.example.roi.model;

import java.util.List;

public class RoiChartData {

    private List<RoiChartDataPoint> dataPoints;
    private Integer paybackYearNum;

    public RoiChartData() {}

    public RoiChartData(List<RoiChartDataPoint> dataPoints, Integer paybackYearNum) {
        this.dataPoints = dataPoints;
        this.paybackYearNum = paybackYearNum;
    }

    // Getters
    public List<RoiChartDataPoint> getDataPoints() {
        return dataPoints;
    }

    public Integer getPaybackYearNum() {
        return paybackYearNum;
    }
}
