package com.example.roi.model;

import java.util.List;

public class RoiCalculationResponseWithPdfFlat {
    private final TotalCost totalCost;
    private final YearlySavings yearlySavings;
    private final MonthlySavings monthlySavings;
    private final PaybackPeriod paybackPeriod;
    private final RoiChartData roiChartData;
    private final RoiPercentage roiPercentage;
    private final List<RoiYearlyBreakdown> yearlyBreakdown;
    private final String pdfUrl;

    public RoiCalculationResponseWithPdfFlat(
            TotalCost totalCost,
            YearlySavings yearlySavings,
            MonthlySavings monthlySavings,
            PaybackPeriod paybackPeriod,
            RoiChartData roiChartData,
            RoiPercentage roiPercentage,
            List<RoiYearlyBreakdown> yearlyBreakdown,
            String pdfUrl) {
        this.totalCost = totalCost;
        this.yearlySavings = yearlySavings;
        this.monthlySavings = monthlySavings;
        this.paybackPeriod = paybackPeriod;
        this.roiChartData = roiChartData;
        this.roiPercentage = roiPercentage;
        this.yearlyBreakdown = yearlyBreakdown;
        this.pdfUrl = pdfUrl;
    }

    public TotalCost getTotalCost() { return totalCost; }
    public YearlySavings getYearlySavings() { return yearlySavings; }
    public MonthlySavings getMonthlySavings() { return monthlySavings; }
    public PaybackPeriod getPaybackPeriod() { return paybackPeriod; }
    public RoiChartData getRoiChartData() { return roiChartData; }
    public RoiPercentage getRoiPercentage() { return roiPercentage; }
    public List<RoiYearlyBreakdown> getYearlyBreakdown() { return yearlyBreakdown; }
    public String getPdfUrl() { return pdfUrl; }
} 