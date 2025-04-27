package com.example.roi.model;

import java.util.List;
import java.util.Map;

/**
 * Model class to hold year-by-year ROI time series data for visualization.
 * This enables payback period visualization showing when ROI becomes positive.
 */
public class TimeSeriesData {
    private Map<String, List<Double>> yearlyData;
    private Map<String, Double> cumulativeData;
    private Map<String, Integer> paybackPeriod;

    public TimeSeriesData(Map<String, List<Double>> yearlyData, Map<String, Double> cumulativeData, Map<String, Integer> paybackPeriod) {
        this.yearlyData = yearlyData;
        this.cumulativeData = cumulativeData;
        this.paybackPeriod = paybackPeriod;
    }

    public Map<String, List<Double>> getYearlyData() {
        return yearlyData;
    }

    public Map<String, Double> getCumulativeData() {
        return cumulativeData;
    }

    public Map<String, Integer> getPaybackPeriod() {
        return paybackPeriod;
    }
} 