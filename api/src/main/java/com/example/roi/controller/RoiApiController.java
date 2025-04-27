package com.example.roi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiResponse;
import com.example.roi.model.TimeSeriesData;
import com.example.roi.service.RoiService;

/**
 * RESTful API controller for ROI calculations.
 * Provides endpoints for returning time series data for visualization.
 */
@RestController
@RequestMapping("/api/roi")
public class RoiApiController {

    @Autowired
    private RoiService roiService;
    
    /**
     * Calculate ROI with time series data for visualization
     * 
     * @param request Contains battery size, usage, and solar size information
     * @return TimeSeriesData object with yearly and cumulative ROI data
     */
    @PostMapping("/calculate")
    public ResponseEntity<TimeSeriesData> calculateRoi(@RequestBody RoiRequest request) {
        try {
            RoiResponse response = roiService.calculate(request);
            return ResponseEntity.ok(response.timeSeriesData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * GET endpoint for quickly viewing time series data for debugging
     * 
     * @param batterySize Battery size in kWh
     * @param usage Annual electricity usage in kWh
     * @param solarSize Solar panel size in kW
     * @return TimeSeriesData object with yearly and cumulative ROI data
     */
    @GetMapping("/timeseries")
    public ResponseEntity<TimeSeriesData> getTimeSeriesData(
            @RequestParam(defaultValue = "17.5") double batterySize,
            @RequestParam(defaultValue = "4000") double usage,
            @RequestParam(defaultValue = "4.0") double solarSize) {
        
        RoiRequest request = new RoiRequest();
        request.setBatterySize(batterySize);
        request.setUsage(usage);
        request.setSolarSize(solarSize);
        
        try {
            RoiResponse response = roiService.calculate(request);
            return ResponseEntity.ok(response.timeSeriesData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 