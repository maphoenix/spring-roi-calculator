package com.example.roi.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiResponse;
import com.example.roi.model.Tariff;
import com.example.roi.model.UserProfile;
import com.example.roi.model.UserProfile.HouseSize;

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

    @Autowired
    private TariffService tariffService;

    /**
     * Derives appropriate RoiRequest defaults based on the user's profile
     * 
     * @param userProfile Contains information about user's house and preferences
     * @return RoiRequest with defaults populated based on user profile
     */
    public RoiRequest deriveRequestDefaults(UserProfile userProfile) {
        RoiRequest request = new RoiRequest();
        
        // Set battery size based on house size
        switch (userProfile.getHouseSize()) {
            case SMALL:
                request.setBatterySize(9.5);
                request.setUsage(2500);
                request.setSolarSize(3.0);
                break;
            case MEDIUM:
                request.setBatterySize(17.5);
                request.setUsage(4000);
                request.setSolarSize(4.0);
                break;
            case LARGE:
                request.setBatterySize(25.0);
                request.setUsage(6000);
                request.setSolarSize(6.0);
                break;
        }
        
        // Adjust for EV presence
        if (userProfile.isHasOrPlanningEv()) {
            request.setUsage(request.getUsage() + 2500); // Add estimated EV usage
            
            // Suggest a larger battery for EV owners if it's not already a large house
            if (userProfile.getHouseSize() != HouseSize.LARGE) {
                request.setBatterySize(Math.min(25.0, request.getBatterySize() * 1.5));
            }
        }
        
        // Adjust for home occupancy during the day
        if (userProfile.isHomeOccupiedDuringDay()) {
            // Higher self-consumption rate suggests more value from larger system
            request.setSolarSize(request.getSolarSize() * 1.2);
        }
        
        return request;
    }

    /**
     * Calculate ROI savings for each tariff based on battery and solar parameters
     * 
     * @param request Contains battery size, usage, and solar size information
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
        double solarExport = solarGen * SOLAR_EXPORT_PERCENTAGE;

        Map<String, Double> results = new HashMap<>();
        
        // Get tariffs from TariffService
        for (Tariff t : tariffService.getAvailableTariffs()) {
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
