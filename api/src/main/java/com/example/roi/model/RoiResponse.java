package com.example.roi.model;

import java.util.Map;

public class RoiResponse {
    public Map<String, Double> totalSavings;

    public RoiResponse(Map<String, Double> totalSavings) {
        this.totalSavings = totalSavings;
    }
}
