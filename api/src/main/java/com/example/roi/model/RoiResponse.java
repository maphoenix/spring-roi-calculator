package com.example.roi.model;

import java.util.Map;

public class RoiResponse {
    public Map<String, Double> totalSavings;
    public TimeSeriesData timeSeriesData;

    public RoiResponse(Map<String, Double> totalSavings) {
        this.totalSavings = totalSavings;
        this.timeSeriesData = null;
    }
    
    public RoiResponse(Map<String, Double> totalSavings, TimeSeriesData timeSeriesData) {
        this.totalSavings = totalSavings;
        this.timeSeriesData = timeSeriesData;
    }
}
