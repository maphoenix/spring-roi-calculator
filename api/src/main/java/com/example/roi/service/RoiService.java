package com.example.roi.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private static final double SOLAR_SELF_USE_PERCENTAGE = 0.50;  // 50% of solar is used directly
    private static final double SOLAR_EXPORT_PERCENTAGE = 0.50;    // 50% of solar is exported
    private static final double AT_HOME_SOLAR_EXPORT_PERCENTAGE = 0.30;    // 30% of solar is exported
    private static final double AT_HOME_SOLAR_SELF_USE_PERCENTAGE = 0.70;  // 70% of solar is used directly

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

        boolean isBatterySelected = request.getBatterySize() > 0;
        boolean isHomeOccupancyDuringWorkHours = request.isHomeOccupancyDuringWorkHours();

        // Get initial total cost of the system
        double initialCost = calculateInitialCost(request);
        TotalCost totalCost = new TotalCost(initialCost);
        logger.info("Initial system cost: £{}", String.format("%.2f", initialCost));

        double originalBatterySize = request.getBatterySize();
        double usableBatteryMaxCapacity = originalBatterySize * BATTERY_USABLE_PERCENTAGE;

        // Solar generation and usage components (constant over years)
        double actualSolarGeneration = SOLAR_GENERATION_FACTOR * getSolarDirectionOutputMultiplier(request.getSolarPanelDirection());
        double solarGen = request.getSolarSize() * actualSolarGeneration;

        double solarUsed;
        double solarExport;
      
     
        logger.info("HomeOccupancyDuringWorkHours: {}",
                String.format("%B", isHomeOccupancyDuringWorkHours));

        if (isHomeOccupancyDuringWorkHours == true) {
            solarUsed = solarGen * AT_HOME_SOLAR_SELF_USE_PERCENTAGE;
            solarExport = solarGen * AT_HOME_SOLAR_EXPORT_PERCENTAGE;
        } else {
            solarUsed = solarGen * SOLAR_SELF_USE_PERCENTAGE;
            solarExport = solarGen * SOLAR_EXPORT_PERCENTAGE;
        }

        logger.info("Solar generation: {}kWh, self-used: {}kWh, exported: {}kWh",
                String.format("%.2f", solarGen),
                String.format("%.2f", solarUsed),
                String.format("%.2f", solarExport));

       Tariff selectedTariff = getTariff(request.isHaveOrWillGetEv());
  
        // --- Year-by-Year Calculation for the selected tariff ---
        List<Double> yearlySavingsList = new ArrayList<>();
        List<RoiChartDataPoint> chartDataPoints = new ArrayList<>();
        List<RoiYearlyBreakdown> yearlyBreakdowns = request.isIncludePdfBreakdown() ? new ArrayList<>() : null;
        double     cumulativeSavings = -initialCost; // Start with negative initial cost
        Integer paybackYearNum = null; // Use Integer to allow null

        logger.info("===== YEAR-BY-YEAR CALCULATION for Tariff: {} =====", selectedTariff.getName());

        for (int year = 1; year <= NUMBER_OF_YEARS_TO_TRACK; year++) {
            double batterySavings = 0;
            double degradationFactor = 1.0;
            double effectiveBatteryCapacity = 0.0;
            double shiftable = 0.0;
            if (isBatterySelected) {
                degradationFactor = calculateBatteryDegradation(year);
                effectiveBatteryCapacity = usableBatteryMaxCapacity * degradationFactor;
                shiftable = Math.min(effectiveBatteryCapacity * 365, request.getUsage());
                batterySavings = calculateBatterySavings(year, usableBatteryMaxCapacity, request, selectedTariff);
            }
            double solarSavingsSelfUse = solarUsed * selectedTariff.getPeakRate();
            double solarSavingsExport = solarExport * selectedTariff.getExportRate();
            double solarSavings = solarSavingsSelfUse + solarSavingsExport;

            double yearTotalSavings = batterySavings + solarSavings;
            yearlySavingsList.add(yearTotalSavings);

            // Update cumulative savings
            cumulativeSavings += yearTotalSavings;

            // Add data point for the chart
            chartDataPoints.add(new RoiChartDataPoint(year, cumulativeSavings));

            // Collect breakdown if requested
            if (yearlyBreakdowns != null) {
                yearlyBreakdowns.add(new RoiYearlyBreakdown(
                    year,
                    usableBatteryMaxCapacity,
                    degradationFactor,
                    shiftable,
                    batterySavings,
                    solarUsed,
                    solarExport,
                    solarSavingsSelfUse,
                    solarSavingsExport,
                    yearTotalSavings,
                    cumulativeSavings
                ));
            }

            // Debug logging with clear prefix
            if (isBatterySelected == true) {
                logger.debug(
                    "[SOLAR+BATT] Year {}: batterySavings=£{}, degradationFactor={}, effectiveBatteryCapacity={}kWh, shiftable={}kWh, solarUsed={}kWh, solarExport={}kWh, solarSavings(self-use)=£{}, solarSavings(export)=£{}, yearTotalSavings=£{}, cumulativeSavings=£{}",
                    year,
                    String.format("%.2f", batterySavings),
                    String.format("%.2f", degradationFactor),
                    String.format("%.2f", effectiveBatteryCapacity),
                    String.format("%.2f", shiftable),
                    String.format("%.2f", solarUsed),
                    String.format("%.2f", solarExport),
                    String.format("%.2f", solarSavingsSelfUse),
                    String.format("%.2f", solarSavingsExport),
                    String.format("%.2f", yearTotalSavings),
                    String.format("%.2f", cumulativeSavings)
                );
            } else {
                logger.debug(
                    "[SOLAR] Year {}: solarUsed={}kWh, solarExport={}kWh, solarSavings(self-use)=£{}, solarSavings(export)=£{}, yearTotalSavings=£{}, cumulativeSavings=£{}",
                    year,
                    String.format("%.2f", solarUsed),
                    String.format("%.2f", solarExport),
                    String.format("%.2f", solarSavingsSelfUse),
                    String.format("%.2f", solarSavingsExport),
                    String.format("%.2f", yearTotalSavings),
                    String.format("%.2f", cumulativeSavings)
                );
            }

            // Check for payback year
            if (paybackYearNum == null && cumulativeSavings > 0) {
                paybackYearNum = year;
                logger.info("PAYBACK ACHIEVED for {} in year {}", selectedTariff.getName(), year);
            }

            if (year == 1 || year % 5 == 0 || year == MAX_BATTERY_YEARS) {
                logger.info("Year {}: Annual savings £{}, Cumulative £{}",
                        year,
                        String.format("%.2f", yearTotalSavings),
                        String.format("%.2f", cumulativeSavings));
            }
        }

        // --- Aggregate Results ---
        // Average Yearly Savings (simple average over the period, excluding years with zero savings if battery degraded fully)
        double averageYearlySavings = yearlySavingsList.stream()
                .filter(s -> s > 0) // Exclude years with zero savings (e.g., after max lifespan)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0); // Handle case where list might be empty or all zeros

        YearlySavings yearlySavings = new YearlySavings(averageYearlySavings);
        MonthlySavings monthlySavings = new MonthlySavings(averageYearlySavings / 12.0);

        // Payback Period
        PaybackPeriod paybackPeriod = new PaybackPeriod(paybackYearNum != null ? paybackYearNum : -1); // -1 indicates no payback within MAX_BATTERY_YEARS

        // ROI Chart Data
        RoiChartData roiChartData = new RoiChartData(chartDataPoints, paybackYearNum);

        // ROI Percentage (Overall ROI at the end of the period)
        double totalSavings = cumulativeSavings + initialCost; // Total profit over initial cost
        double roiPercent = (initialCost > 0) ? (totalSavings / initialCost) * 100 : 0;
        RoiPercentage roiPercentage = new RoiPercentage(roiPercent, MAX_BATTERY_YEARS);

        logger.info("===== CALCULATION SUMMARY =====");
        logger.info("Total Cost: £{}", String.format("%.2f", totalCost.getAmount()));
        logger.info("Average Yearly Savings: £{}", String.format("%.2f", yearlySavings.getAmount()));
        logger.info("Average Monthly Savings: £{}", String.format("%.2f", monthlySavings.getAmount()));
        logger.info("Payback Period: {} years", paybackPeriod.getYears() == -1 ? "N/A" : paybackPeriod.getYears());
        logger.info("Final Cumulative Savings (Year {}): £{}", MAX_BATTERY_YEARS, String.format("%.2f", cumulativeSavings));
        logger.info("Overall ROI ({} years): {}%", roiPercentage.getPeriodYears(), String.format("%.2f", roiPercentage.getPercentage()));

        // Construct and return the final response object
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
