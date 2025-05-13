package com.example.roi;

/**
 * Represents the result of ROI calculations for a single year.
 * Holds all relevant savings, degradation, and breakdown values for that year.
 */
public class YearCalculationResult {
    /** Battery savings for the year (£) */
    public final double batterySavings;
    /** Battery degradation factor for the year (0-1) */
    public final double degradationFactor;
    /** Effective battery capacity for the year (kWh) */
    public final double effectiveBatteryCapacity;
    /** Shiftable energy for the year (kWh) */
    public final double shiftable;
    /** Solar savings from self-use for the year (£) */
    public final double solarSavingsSelfUse;
    /** Solar savings from export for the year (£) */
    public final double solarSavingsExport;
    /** Total savings for the year (£) */
    public final double yearTotalSavings;

    /**
     * Constructs a YearCalculationResult instance.
     *
     * @param batterySavings           Battery savings for the year (£)
     * @param degradationFactor        Battery degradation factor for the year (0-1)
     * @param effectiveBatteryCapacity Effective battery capacity for the year (kWh)
     * @param shiftable                Shiftable energy for the year (kWh)
     * @param solarSavingsSelfUse      Solar savings from self-use for the year (£)
     * @param solarSavingsExport       Solar savings from export for the year (£)
     * @param yearTotalSavings         Total savings for the year (£)
     */
    public YearCalculationResult(double batterySavings, double degradationFactor, double effectiveBatteryCapacity, double shiftable, double solarSavingsSelfUse, double solarSavingsExport, double yearTotalSavings) {
        this.batterySavings = batterySavings;
        this.degradationFactor = degradationFactor;
        this.effectiveBatteryCapacity = effectiveBatteryCapacity;
        this.shiftable = shiftable;
        this.solarSavingsSelfUse = solarSavingsSelfUse;
        this.solarSavingsExport = solarSavingsExport;
        this.yearTotalSavings = yearTotalSavings;
    }
} 