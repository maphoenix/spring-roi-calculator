package com.example.roi.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiResponse;
import com.example.roi.model.Tariff;

/**
 * Service for calculating Return on Investment (ROI) for battery and solar installations
 * based on different tariff structures.
 * 
 * The calculation methodology is documented in calculations.md
 */
@Service
public class RoiService {
    // Constants for calculations
    private static final double BATTERY_USABLE_PERCENTAGE = 0.90;  // 90% of battery is usable
    private static final double BATTERY_EFFICIENCY = 0.85;         // 85% round-trip efficiency
    private static final double SOLAR_GENERATION_FACTOR = 850.0;   // kWh per kW of solar annually
    private static final double SOLAR_SELF_USE_PERCENTAGE = 0.50;  // 50% of solar is used directly
    private static final double SOLAR_EXPORT_PERCENTAGE = 0.50;    // 50% of solar is exported
    private static final double GRID_EXPORT_EFFICIENCY = 0.60;     // 60% efficiency for exported power

    /**
     * Calculate ROI savings for each tariff based on battery and solar parameters
     * 
     * @param request Contains battery size, usage, solar size, and tariff information
     * @return Response containing total savings for each tariff
     */
    public RoiResponse calculate(RoiRequest request) {
        // Step 1: Calculate battery savings components
        double usableBattery = request.getBatterySize() * BATTERY_USABLE_PERCENTAGE;
        
        // Shiftable energy is limited by either daily battery capacity over a year or total annual usage
        double shiftable = Math.min(usableBattery * 365, request.getUsage());
        
        // Step 2: Calculate solar generation and usage components
        double solarGen = request.getSolarSize() * SOLAR_GENERATION_FACTOR;
        double solarUsed = solarGen * SOLAR_SELF_USE_PERCENTAGE;
        double solarExport = solarGen * SOLAR_EXPORT_PERCENTAGE * GRID_EXPORT_EFFICIENCY;

        Map<String, Double> results = new HashMap<>();
        
        // Add null check for tariffs list
        if (request.getTariffs() == null) {
            return new RoiResponse(results);
        }
        
        // Calculate savings for each tariff
        for (Tariff t : request.getTariffs()) {
            // Step 3: Calculate battery savings (arbitrage between peak and off-peak rates)
            // Formula: Shiftable Energy × (Peak Rate - Offpeak Rate) × Battery Efficiency
            double batterySavings = shiftable * (t.getPeakRate() - t.getOffpeakRate()) * BATTERY_EFFICIENCY;
            
            // Step 4: Calculate solar savings (self-use and export)
            // Formula: (Solar Self-Used × Peak Rate) + (Solar Exported × Export Rate)
            double solarSavings = (solarUsed * t.getPeakRate()) + (solarExport * t.getExportRate());
            
            // Step 5: Calculate total savings
            double total = batterySavings + solarSavings;
            
            results.put(t.getName(), total);
        }
        
        return new RoiResponse(results);
    }
}
