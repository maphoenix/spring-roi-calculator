package com.example.roi.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiResponse;
import com.example.roi.model.Tariff;
import com.example.roi.model.TimeSeriesData;
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
    private static final Logger logger = LoggerFactory.getLogger(RoiService.class);
    
    // Constants for calculations
    private static final double BATTERY_USABLE_PERCENTAGE = 0.90;  // 90% of battery is usable
    private static final double BATTERY_EFFICIENCY = 0.85;         // 85% round-trip efficiency
    private static final double SOLAR_GENERATION_FACTOR = 850.0;   // kWh per kW of solar annually
    private static final double SOLAR_SELF_USE_PERCENTAGE = 0.50;  // 50% of solar is used directly
    private static final double SOLAR_EXPORT_PERCENTAGE = 0.50;    // 50% of solar is exported
    private static final int MAX_BATTERY_YEARS = 15;               // Maximum battery lifespan in years
    private static final double BATTERY_YEAR_10_CAPACITY = 0.70;   // Battery at 70% capacity after 10 years
    
    // Initial cost estimates (could be parameterized in future versions)
    private static final double BATTERY_COST_PER_KWH = 500.0;      // Cost per kWh of battery
    private static final double SOLAR_COST_PER_KW = 1500.0;        // Cost per kW of solar

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
     * Calculate battery degradation factor for a specific year
     * Based on the fact that batteries are at 70% capacity after 10 years
     * and we assume they won't live longer than 15 years
     * 
     * @param year The year for which to calculate degradation (1-indexed)
     * @return Degradation factor (percentage of original capacity)
     */
    private double calculateBatteryDegradation(int year) {
        if (year <= 0) {
            return 1.0; // No degradation at installation/year 0
        }
        
        if (year > MAX_BATTERY_YEARS) {
            return 0.0; // No capacity after max years
        }
        
        // Linear degradation model based on 70% capacity at year 10
        if (year <= 10) {
            // Linear decline to 70% by year 10
            return 1.0 - ((1.0 - BATTERY_YEAR_10_CAPACITY) * year / 10.0);
        } else {
            // Accelerated decline after year 10
            double remainingYears = MAX_BATTERY_YEARS - 10;
            double remainingCapacity = BATTERY_YEAR_10_CAPACITY;
            double yearsPastTen = year - 10;
            
            return BATTERY_YEAR_10_CAPACITY - (remainingCapacity * yearsPastTen / remainingYears);
        }
    }

    /**
     * Calculate ROI savings for each tariff based on battery and solar parameters
     * 
     * @param request Contains battery size, usage, and solar size information
     * @return Response containing total savings for each tariff
     */
    public RoiResponse calculate(RoiRequest request) {
        logger.info("Calculating ROI for battery: {}kWh, solar: {}kW, usage: {}kWh", 
                request.getBatterySize(), request.getSolarSize(), request.getUsage());
        
        // Get initial total cost of the system
        double initialCost = (request.getBatterySize() * BATTERY_COST_PER_KWH) + 
                             (request.getSolarSize() * SOLAR_COST_PER_KW);
        
        logger.info("Initial system cost: £{}", String.format("%.2f", initialCost));
        
        double originalBatterySize = request.getBatterySize();
        double usableBatteryMaxCapacity = originalBatterySize * BATTERY_USABLE_PERCENTAGE;
        
        // For returning single value totals as before
        Map<String, Double> totalResults = new HashMap<>();
        
        // For time series data
        Map<String, List<Double>> yearlyData = new HashMap<>();
        Map<String, Double> cumulativeTotal = new HashMap<>();
        Map<String, Integer> paybackYear = new HashMap<>();
        
        // Solar generation and usage components (constant over years)
        double solarGen = request.getSolarSize() * SOLAR_GENERATION_FACTOR;
        double solarUsed = solarGen * SOLAR_SELF_USE_PERCENTAGE;
        double solarExport = solarGen * SOLAR_EXPORT_PERCENTAGE;
        
        logger.info("Solar generation: {}kWh, self-used: {}kWh, exported: {}kWh",
                String.format("%.2f", solarGen), 
                String.format("%.2f", solarUsed), 
                String.format("%.2f", solarExport));
        
        // Get tariffs from TariffService
        List<Tariff> tariffs = tariffService.getAvailableTariffs();
        
        // Initialize yearly data lists for each tariff
        for (Tariff t : tariffs) {
            yearlyData.put(t.getName(), new ArrayList<>());
            cumulativeTotal.put(t.getName(), -initialCost); // Start with negative initial cost
            paybackYear.put(t.getName(), -1); // Default to -1 (no payback)
        }
        
        logger.info("===== YEAR-BY-YEAR CALCULATION =====");
        
        // Calculate year-by-year data
        for (int year = 1; year <= MAX_BATTERY_YEARS; year++) {
            // Apply degradation factor for this year
            double degradationFactor = calculateBatteryDegradation(year);
            double effectiveBatteryCapacity = usableBatteryMaxCapacity * degradationFactor;
            
            logger.debug("Year {}: Battery degradation {}%, effective capacity: {}kWh", 
                    year, String.format("%.1f", degradationFactor * 100), 
                    String.format("%.2f", effectiveBatteryCapacity));
            
            // Shiftable energy is limited by daily battery capacity over a year or total usage
            double shiftable = Math.min(effectiveBatteryCapacity * 365, request.getUsage());
            
            // Calculate per-tariff values
            for (Tariff t : tariffs) {
                // Battery savings (arbitrage between peak and off-peak rates)
                double batterySavings = shiftable * (t.getPeakRate() - t.getOffpeakRate()) * BATTERY_EFFICIENCY;
                
                // Solar savings (self-use and export)
                double solarSavings = (solarUsed * t.getPeakRate()) + (solarExport * t.getExportRate());
                
                // Total savings for this year
                double yearTotal = batterySavings + solarSavings;
                
                // Add to yearly data list
                yearlyData.get(t.getName()).add(yearTotal);
                
                // Update cumulative total
                double newCumulative = cumulativeTotal.get(t.getName()) + yearTotal;
                cumulativeTotal.put(t.getName(), newCumulative);
                
                // Check if this is the payback year (first year where cumulative becomes positive)
                if (paybackYear.get(t.getName()) == -1 && newCumulative > 0) {
                    paybackYear.put(t.getName(), year);
                    logger.info("PAYBACK ACHIEVED for {} in year {}", t.getName(), year);
                }
                
                // For year 1, also save to the original format for backward compatibility
                if (year == 1) {
                    totalResults.put(t.getName(), yearTotal);
                }
                
                if (year % 5 == 0 || year == 1) {
                    logger.info("Year {}, Tariff {}: Annual savings £{}, Cumulative £{}", 
                            year, t.getName(), 
                            String.format("%.2f", yearTotal),
                            String.format("%.2f", newCumulative));
                }
            }
        }
        
        // Log summary of results
        logger.info("===== SUMMARY OF PAYBACK PERIODS =====");
        for (Map.Entry<String, Integer> entry : paybackYear.entrySet()) {
            String paybackStatus = entry.getValue() == -1 ? 
                    "No payback within " + MAX_BATTERY_YEARS + " years" : 
                    "Payback in year " + entry.getValue();
            
            logger.info("Tariff: {}, {}, Final ROI: £{}", 
                    entry.getKey(), 
                    paybackStatus,
                    String.format("%.2f", cumulativeTotal.get(entry.getKey())));
        }
        
        // Create TimeSeriesData object with all the calculated data
        TimeSeriesData timeSeriesData = new TimeSeriesData(yearlyData, cumulativeTotal, paybackYear);
        
        // Return response with both old and new formats
        return new RoiResponse(totalResults, timeSeriesData);
    }
}
