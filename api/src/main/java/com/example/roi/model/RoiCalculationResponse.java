package com.example.roi.model;

import java.util.List;

// No List import needed here
// No need to import classes from the same package
// This response class aggregates all the calculated ROI metrics
// based on the client-side TypeScript types.
public class RoiCalculationResponse {

    private TotalCost totalCost;
    private YearlySavings yearlySavings;
    private MonthlySavings monthlySavings;
    private PaybackPeriod paybackPeriod;
    private RoiChartData roiChartData;
    private RoiPercentage roiPercentage;
    private List<RoiYearlyBreakdown> yearlyBreakdown;

    // No-args constructor for Jackson
    public RoiCalculationResponse() {}

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
