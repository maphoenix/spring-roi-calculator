package com.example.roi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.roi.model.RoiCalculationResponse;
import com.example.roi.model.RoiRequest;
import com.example.roi.service.RoiService;

/**
 * RESTful API controller for ROI calculations. Provides endpoints for returning
 * ROI metrics for visualization.
 */
@RestController
@RequestMapping("/api/roi")
public class RoiApiController {

    @Autowired
    private RoiService roiService;

    /**
     * Calculate ROI with aggregated metrics for visualization
     *
     * @param request Contains battery size, usage, solar size, and other user
     * inputs
     * @return RoiCalculationResponse object with various ROI metrics
     */
    @PostMapping("/calculate")
    public ResponseEntity<RoiCalculationResponse> calculateRoi(@RequestBody RoiRequest request) {
        try {
            RoiCalculationResponse response = roiService.calculate(request);
            if (response == null) {
                // Handle cases where calculation couldn't be performed (e.g., no tariffs)
                return ResponseEntity.internalServerError().build(); // Or badRequest, depending on cause
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the exception details for debugging
            // logger.error("Error during ROI calculation", e);
            return ResponseEntity.internalServerError().build(); // Use 500 for unexpected errors
        }
    }

    /**
     * GET endpoint for quickly viewing ROI data for debugging with default
     * parameters
     *
     * @param batterySize Battery size in kWh
     * @param usage Annual electricity usage in kWh
     * @param solarSize Solar panel size in kW
     * @return RoiCalculationResponse object with ROI metrics
     */
    @GetMapping("/timeseries")
    public ResponseEntity<RoiCalculationResponse> getTimeSeriesData(
            @RequestParam(defaultValue = "17.5") double batterySize,
            @RequestParam(defaultValue = "4000") double usage,
            @RequestParam(defaultValue = "4.0") double solarSize) {

        // Note: This endpoint ignores the newer request parameters like direction, EV, etc.
        // It only uses the basic parameters for a quick check.
        RoiRequest request = new RoiRequest();
        request.setBatterySize(batterySize);
        request.setUsage(usage);
        request.setSolarSize(solarSize);

        try {
            RoiCalculationResponse response = roiService.calculate(request);
            if (response == null) {
                return ResponseEntity.internalServerError().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // logger.error("Error during ROI calculation (GET endpoint)", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
