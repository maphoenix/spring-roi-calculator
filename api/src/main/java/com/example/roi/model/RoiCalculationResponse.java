package com.example.roi.model;

import java.util.List;

// No List import needed here
// No need to import classes from the same package
// This response class aggregates all the calculated ROI metrics
// based on the client-side TypeScript types.
public class RoiCalculationResponse {

    private final TotalCost totalCost; // Added missing field and made final
    private final YearlySavings yearlySavings; // Made final
    private final MonthlySavings monthlySavings; // Made final
    private final PaybackPeriod paybackPeriod; // Made final
    private final RoiChartData roiChartData; // Made final
    private final RoiPercentage roiPercentage; // Made final
    private final List<RoiYearlyBreakdown> yearlyBreakdown;

    // Constructor
    public RoiCalculationResponse(TotalCost totalCost, YearlySavings yearlySavings, MonthlySavings monthlySavings,
            PaybackPeriod paybackPeriod, RoiChartData roiChartData, RoiPercentage roiPercentage) {
        this(totalCost, yearlySavings, monthlySavings, paybackPeriod, roiChartData, roiPercentage, null);
    }

    public RoiCalculationResponse(TotalCost totalCost, YearlySavings yearlySavings, MonthlySavings monthlySavings,
            PaybackPeriod paybackPeriod, RoiChartData roiChartData, RoiPercentage roiPercentage, List<RoiYearlyBreakdown> yearlyBreakdown) {
        this.totalCost = totalCost;
        this.yearlySavings = yearlySavings;
        this.monthlySavings = monthlySavings;
        this.paybackPeriod = paybackPeriod;
        this.roiChartData = roiChartData;
        this.roiPercentage = roiPercentage;
        this.yearlyBreakdown = yearlyBreakdown;
    }

    // Getters
    public TotalCost getTotalCost() {
        return totalCost;
    }

    public YearlySavings getYearlySavings() {
        return yearlySavings;
    }

    public MonthlySavings getMonthlySavings() {
        return monthlySavings;
    }

    public PaybackPeriod getPaybackPeriod() {
        return paybackPeriod;
    }

    public RoiChartData getRoiChartData() {
        return roiChartData;
    }

    public RoiPercentage getRoiPercentage() {
        return roiPercentage;
    }

    public List<RoiYearlyBreakdown> getYearlyBreakdown() {
        return yearlyBreakdown;
    }
}
