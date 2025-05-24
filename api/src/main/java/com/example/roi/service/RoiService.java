package com.example.roi.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.roi.SolarInfo;
import com.example.roi.YearCalculationResult;
import com.example.roi.mcs.McsLookup;
import com.example.roi.model.MonthlySavings;
import com.example.roi.model.PaybackPeriod;
import com.example.roi.model.RoiCalculationResponse;
import com.example.roi.model.RoiChartData;
import com.example.roi.model.RoiChartDataPoint;
import com.example.roi.model.RoiPercentage;
import com.example.roi.model.RoiRequest;
import com.example.roi.model.RoiYearlyBreakdown;
import com.example.roi.model.Tariff;
import com.example.roi.model.TotalCost;
import com.example.roi.model.YearlySavings;

/**
 * Service for calculating Return on Investment (ROI) for battery and solar
 * installations based on different tariff structures.
 *
 * The calculation methodology is documented in calculations.md
 */
@Service
public class RoiService {

    private static final Logger logger = LoggerFactory.getLogger(RoiService.class);

    // Constants for calculations
    private static final double BATTERY_USABLE_PERCENTAGE = 0.90;  // 90% of battery is usable
    
    // Initial cost estimates (could be parameterized in future versions)
    private static final double SOLAR_GENERATION_FACTOR = 850.0;   // kWh per kW of solar annually
    private static final double BATTERY_EFFICIENCY = 0.85;         // 85% round-trip efficiency
    private static final int MAX_BATTERY_YEARS = 15;               // Maximum battery lifespan in years
    private static final double BATTERY_YEAR_10_CAPACITY = 0.70;   // Battery at 70% capacity after 10 years
    private static final double BATTERY_COST_PER_KWH = 500.0;      // Cost per kWh of battery
    private static final double SOLAR_COST_PER_KW = 1500.0;        // Cost per kW of solar

    private static final int NUMBER_OF_YEARS_TO_TRACK = 15;

    @Autowired
    private TariffService tariffService;
    
    private McsLookup mcsLookup;
    
    // Initialize McsLookup with the CSV data
    public RoiService() {
        try {
            // Load the MCS lookup data from CSV file in resources
            String csvPath = getClass().getClassLoader().getResource("mcs/mcs_synthetic_dataset.csv").getPath();
            this.mcsLookup = new McsLookup(csvPath);
        } catch (Exception e) {
            logger.warn("Failed to load MCS lookup data, falling back to simple calculations: {}", e.getMessage());
            this.mcsLookup = null;
        }
    }

    /**
     * Calculate battery degradation factor for a specific year Based on the
     * fact that batteries are at 70% capacity after 10 years and we assume they
     * won't live longer than 15 years
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
     * Calculate battery savings for a specific year Based on the
     * fact that batteries are at 70% capacity after 10 years and we assume they
     * won't live longer than 15 years
     *
     * @param year The year for which to calculate savings
     * @param usableBatteryMaxCapacity The maximum usable capacity of the battery
     * @param request The RoiRequest containing battery size, usage, and solar size information
     * @param selectedTariff The selected tariff for the calculation
     * @return The calculated battery savings
     */
    private double calculateBatterySavings(int year, double usableBatteryMaxCapacity, RoiRequest request, Tariff selectedTariff) {
        double degradationFactor = calculateBatteryDegradation(year);
        double effectiveBatteryCapacity = usableBatteryMaxCapacity * degradationFactor;
        // Shiftable energy is limited by daily battery capacity over a year or total usage
        double shiftable = Math.min(effectiveBatteryCapacity * 365, request.getUsage());
        // Battery savings (arbitrage)
        return shiftable * (selectedTariff.getPeakRate() - selectedTariff.getOffpeakRate()) * BATTERY_EFFICIENCY;
    }

    /**
     * Calculate ROI savings based on battery and solar parameters for a single
     * chosen tariff.
     *
     * @param request Contains battery size, usage, and solar size information
     * @return Response containing aggregated ROI metrics
     */
    public RoiCalculationResponse calculate(RoiRequest request) {
        logger.info(request.toString());

        // Step 1: Extract and prepare input parameters
        boolean isBatterySelected = request.getBatterySize() > 0;
        int occupancyDays = request.getHomeOccupancyDuringWorkHours();

        // Step 2: Calculate initial system cost
        double initialCost = calculateInitialCost(request);
        TotalCost totalCost = new TotalCost(initialCost);
        logger.info("Initial system cost: £{}", String.format("%.2f", initialCost));

        // Step 3: Calculate usable battery capacity (if battery is selected)
        double usableBatteryMaxCapacity = request.getBatterySize() * BATTERY_USABLE_PERCENTAGE;

        // Step 4: Calculate solar generation, self-use, and export
        SolarInfo solarInfo = calculateSolarInfo(request, occupancyDays);
        logSolarInfo(solarInfo);

        // Step 5: Select the appropriate tariff for calculation
        Tariff selectedTariff = getTariff(request.isHaveOrWillGetEv());

        // Step 6: Initialize data structures for results
        List<Double> yearlySavingsList = new ArrayList<>();
        List<RoiChartDataPoint> chartDataPoints = new ArrayList<>();
        List<RoiYearlyBreakdown> yearlyBreakdowns = request.isIncludePdfBreakdown() ? new ArrayList<>() : null;
        double cumulativeSavings = -initialCost; // Start with negative initial cost
        Integer paybackYearNum = null;

        logger.info("===== YEAR-BY-YEAR CALCULATION for Tariff: {} =====", selectedTariff.getName());

        // Step 7: Perform year-by-year calculation and collect results
        for (int year = 1; year <= NUMBER_OF_YEARS_TO_TRACK; year++) {
            YearCalculationResult yearResult = calculateYear(
                year,
                isBatterySelected,
                usableBatteryMaxCapacity,
                request,
                selectedTariff,
                solarInfo
            );

            // Track yearly savings and update cumulative savings
            yearlySavingsList.add(yearResult.yearTotalSavings);
            cumulativeSavings += yearResult.yearTotalSavings;
            chartDataPoints.add(new RoiChartDataPoint(year, cumulativeSavings));

            // Optionally collect detailed breakdown for PDF
            if (yearlyBreakdowns != null) {
                yearlyBreakdowns.add(new RoiYearlyBreakdown(
                    year,
                    usableBatteryMaxCapacity,
                    yearResult.degradationFactor,
                    yearResult.shiftable,
                    yearResult.batterySavings,
                    solarInfo.solarUsed,
                    solarInfo.solarExport,
                    yearResult.solarSavingsSelfUse,
                    yearResult.solarSavingsExport,
                    yearResult.yearTotalSavings,
                    cumulativeSavings
                ));
            }

            logYearDebug(isBatterySelected, year, yearResult, solarInfo, cumulativeSavings);

            // Check for payback year
            if (paybackYearNum == null && cumulativeSavings > 0) {
                paybackYearNum = year;
                logger.info("PAYBACK ACHIEVED for {} in year {}", selectedTariff.getName(), year);
            }

            // Log summary at key years
            if (year == 1 || year % 5 == 0 || year == MAX_BATTERY_YEARS) {
                logger.info("Year {}: Annual savings £{}, Cumulative £{}",
                        year,
                        String.format("%.2f", yearResult.yearTotalSavings),
                        String.format("%.2f", cumulativeSavings));
            }
        }

        // Step 8: Aggregate results for response
        double averageYearlySavings = yearlySavingsList.stream()
                .filter(s -> s > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        YearlySavings yearlySavings = new YearlySavings(averageYearlySavings);
        MonthlySavings monthlySavings = new MonthlySavings(averageYearlySavings / 12.0);
        PaybackPeriod paybackPeriod = new PaybackPeriod(paybackYearNum != null ? paybackYearNum : -1);
        RoiChartData roiChartData = new RoiChartData(chartDataPoints, paybackYearNum);
        double totalSavings = cumulativeSavings + initialCost;
        double roiPercent = (initialCost > 0) ? (totalSavings / initialCost) * 100 : 0;
        RoiPercentage roiPercentage = new RoiPercentage(roiPercent, MAX_BATTERY_YEARS);

        logSummary(totalCost, yearlySavings, monthlySavings, paybackPeriod, cumulativeSavings, roiPercentage);

        // Step 9: Construct and return the final response object
        return new RoiCalculationResponse(
                totalCost,
                yearlySavings,
                monthlySavings,
                paybackPeriod,
                roiChartData,
                roiPercentage,
                yearlyBreakdowns
        );
    }

    private SolarInfo calculateSolarInfo(RoiRequest request, int occupancyDays) {
        double actualSolarGeneration = SOLAR_GENERATION_FACTOR * getSolarDirectionOutputMultiplier(request.getSolarPanelDirection());
        double solarGen = request.getSolarSize() * actualSolarGeneration;
        double solarUsed;
        double solarExport;
        
        // Use MCS lookup table for accurate self-consumption percentage
        if (mcsLookup != null) {
            try {
                // Get self-consumption percentage from MCS data
                double selfConsumptionPercentage = mcsLookup.lookup(
                    occupancyDays,
                    request.getUsage(),  // annual consumption
                    solarGen,           // PV generation
                    request.getBatterySize()
                );
                
                // Convert percentage to decimal and calculate actual values
                double selfConsumptionRatio = selfConsumptionPercentage / 100.0;
                solarUsed = solarGen * selfConsumptionRatio;
                solarExport = solarGen - solarUsed;
                
                logger.debug("MCS Lookup: occupancyDays={}, consumption={}kWh, pvGen={}kWh, battery={}kWh -> {}% self-consumption", 
                    occupancyDays, request.getUsage(), solarGen, request.getBatterySize(), selfConsumptionPercentage);
                
            } catch (Exception e) {
                logger.warn("Failed to use MCS lookup, falling back to simple calculations: {}", e.getMessage());
                throw new IllegalStateException("The data is not in range for the MCS lookup");
            }
        } else {
            // MCS lookup is required for accurate calculations
            throw new IllegalStateException("The data is not in range for the MCS lookup");
        }
        
        return new SolarInfo(solarGen, solarUsed, solarExport);
    }


    private YearCalculationResult calculateYear(int year, boolean isBatterySelected, double usableBatteryMaxCapacity, RoiRequest request, Tariff selectedTariff, SolarInfo solarInfo) {
        double batterySavings = 0.0;
        double degradationFactor = 1.0;
        double effectiveBatteryCapacity = 0.0;
        double shiftable = 0.0;
        if (isBatterySelected) {
            degradationFactor = calculateBatteryDegradation(year);
            effectiveBatteryCapacity = usableBatteryMaxCapacity * degradationFactor;
            shiftable = Math.min(effectiveBatteryCapacity * 365, request.getUsage());
            batterySavings = calculateBatterySavings(year, usableBatteryMaxCapacity, request, selectedTariff);
        }
        double solarSavingsSelfUse = solarInfo.solarUsed * selectedTariff.getPeakRate();
        double solarSavingsExport = solarInfo.solarExport * selectedTariff.getExportRate();
        double yearTotalSavings = batterySavings + solarSavingsSelfUse + solarSavingsExport;
        return new YearCalculationResult(batterySavings, degradationFactor, effectiveBatteryCapacity, shiftable, solarSavingsSelfUse, solarSavingsExport, yearTotalSavings);
    }

    private void logSolarInfo(SolarInfo solarInfo) {
        logger.info("Solar generation: {}kWh, self-used: {}kWh, exported: {}kWh",
                String.format("%.2f", solarInfo.solarGen),
                String.format("%.2f", solarInfo.solarUsed),
                String.format("%.2f", solarInfo.solarExport));
    }

    private void logYearDebug(boolean isBatterySelected, int year, YearCalculationResult yearResult, SolarInfo solarInfo, double cumulativeSavings) {
        if (isBatterySelected) {
            logger.debug(
                "[SOLAR+BATT] Year {}: batterySavings=£{}, degradationFactor={}, effectiveBatteryCapacity={}kWh, shiftable={}kWh, solarUsed={}kWh, solarExport={}kWh, solarSavings(self-use)=£{}, solarSavings(export)=£{}, yearTotalSavings=£{}, cumulativeSavings=£{}",
                year,
                String.format("%.2f", yearResult.batterySavings),
                String.format("%.2f", yearResult.degradationFactor),
                String.format("%.2f", yearResult.effectiveBatteryCapacity),
                String.format("%.2f", yearResult.shiftable),
                String.format("%.2f", solarInfo.solarUsed),
                String.format("%.2f", solarInfo.solarExport),
                String.format("%.2f", yearResult.solarSavingsSelfUse),
                String.format("%.2f", yearResult.solarSavingsExport),
                String.format("%.2f", yearResult.yearTotalSavings),
                String.format("%.2f", cumulativeSavings)
            );
        } else {
            logger.debug(
                "[SOLAR] Year {}: solarUsed={}kWh, solarExport={}kWh, solarSavings(self-use)=£{}, solarSavings(export)=£{}, yearTotalSavings=£{}, cumulativeSavings=£{}",
                year,
                String.format("%.2f", solarInfo.solarUsed),
                String.format("%.2f", solarInfo.solarExport),
                String.format("%.2f", yearResult.solarSavingsSelfUse),
                String.format("%.2f", yearResult.solarSavingsExport),
                String.format("%.2f", yearResult.yearTotalSavings),
                String.format("%.2f", cumulativeSavings)
            );
        }
    }

    private void logSummary(TotalCost totalCost, YearlySavings yearlySavings, MonthlySavings monthlySavings, PaybackPeriod paybackPeriod, double cumulativeSavings, RoiPercentage roiPercentage) {
        logger.info("===== CALCULATION SUMMARY =====");
        logger.info("Total Cost: £{}", String.format("%.2f", totalCost.getAmount()));
        logger.info("Average Yearly Savings: £{}", String.format("%.2f", yearlySavings.getAmount()));
        logger.info("Average Monthly Savings: £{}", String.format("%.2f", monthlySavings.getAmount()));
        logger.info("Payback Period: {} years", paybackPeriod.getYears() == -1 ? "N/A" : paybackPeriod.getYears());
        logger.info("Final Cumulative Savings (Year {}): £{}", MAX_BATTERY_YEARS, String.format("%.2f", cumulativeSavings));
        logger.info("Overall ROI ({} years): {}%", roiPercentage.getPeriodYears(), String.format("%.2f", roiPercentage.getPercentage()));
    }

    private double calculateInitialCost(RoiRequest request) {
        boolean isBatterySelected = (request.getBatterySize() > 0);
        if (isBatterySelected) {
            return (request.getBatterySize() * BATTERY_COST_PER_KWH)
                + (request.getSolarSize() * SOLAR_COST_PER_KW);
        } else {
            return (request.getSolarSize() * SOLAR_COST_PER_KW);
        }
    }

    /**
     * Returns the typical output multiplier for a given solar panel direction.
     * Multiplier is relative to south-facing (1.0 = 100% of optimal output).
     *
     * @param direction The cardinal direction the panels face
     * @return Output multiplier (e.g., 1.0 for south, 0.8 for east/west, etc.)
     */
    public static double getSolarDirectionOutputMultiplier(RoiRequest.CardinalDirection direction) {
        if (direction == null) {
            return 1.0;
        }
        return switch (direction) {
            case SOUTH ->
                1.0;
            case SOUTH_EAST, SOUTH_WEST ->
                0.97;
            case EAST, WEST ->
                0.83;
            case NORTH_EAST, NORTH_WEST ->
                0.73;
            case NORTH ->
                0.63;
            default ->
                1.0;
        };
    }

    private Tariff getTariff (Boolean  needsEvTariff) {


        List<Tariff> tariffs = tariffService.getAvailableTariffs();
        if (tariffs == null || tariffs.isEmpty()) { 
            logger.error("No tariffs available for calculation.");
            throw new IllegalStateException("No tariffs available for calculation.");
        }

        Tariff selectedTariff = tariffs.stream()
                .filter(tariff -> tariff.isEvRequired() == needsEvTariff)
                .findFirst()
                .orElseThrow(() -> {
                    String message = String.format("No suitable tariff found for EV status: %s", needsEvTariff);
                    logger.error(message);
                    return new IllegalStateException(message);
                });

        logger.info("Using tariff for calculation: {} (Tariff: {})", selectedTariff.getName(), selectedTariff.isEvRequired());

        return selectedTariff;  
    }
}

