package com.example.roi;

/**
 * Represents solar generation information for ROI calculations.
 * Holds the total solar generation, the amount used on-site, and the amount exported.
 */
public class SolarInfo {
    /** Total solar generation (kWh/year) */
    public final double solarGen;
    /** Solar energy used on-site (kWh/year) */
    public final double solarUsed;
    /** Solar energy exported to the grid (kWh/year) */
    public final double solarExport;

    /**
     * Constructs a SolarInfo instance.
     *
     * @param solarGen   Total solar generation (kWh/year)
     * @param solarUsed  Solar energy used on-site (kWh/year)
     * @param solarExport Solar energy exported to the grid (kWh/year)
     */
    public SolarInfo(double solarGen, double solarUsed, double solarExport) {
        this.solarGen = solarGen;
        this.solarUsed = solarUsed;
        this.solarExport = solarExport;
    }
} 