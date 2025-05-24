package com.example.roi.mcs;

import java.io.Serializable;

/**
 * Serializable representation of an MCS (Microgeneration Certification Scheme) entry.
 * This class is used for caching MCS lookup data in a binary format for faster loading
 * compared to parsing large CSV files.
 * 
 * The class implements Serializable to support Java object serialization.
 * All fields are final for immutability and thread safety.
 */
public class McsEntry implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public final int occupancyDays;
    public final double occupancyDaysNormalized;
    public final double annualConsumptionKwh;
    public final double pvGenerationKwh;
    public final double batterySizeKwh;
    public final double predictedSelfConsumptionPercentage;
    public final double pvToConsumptionRatio;
    public final double batteryToConsumptionRatio;
    
    /**
     * Creates a new McsEntry with the specified parameters.
     *
     * @param occupancyDays Number of days at home (1-5)
     * @param occupancyDaysNormalized Normalized occupancy (0.2-1.0)
     * @param annualConsumptionKwh Annual consumption in kWh
     * @param pvGenerationKwh Annual PV generation in kWh
     * @param batterySizeKwh Battery size in kWh
     * @param predictedSelfConsumptionPercentage Self-consumption percentage (0-100)
     * @param pvToConsumptionRatio PV to consumption ratio
     * @param batteryToConsumptionRatio Battery to consumption ratio
     */
    public McsEntry(int occupancyDays, double occupancyDaysNormalized, 
                   double annualConsumptionKwh, double pvGenerationKwh,
                   double batterySizeKwh, double predictedSelfConsumptionPercentage,
                   double pvToConsumptionRatio, double batteryToConsumptionRatio) {
        this.occupancyDays = occupancyDays;
        this.occupancyDaysNormalized = occupancyDaysNormalized;
        this.annualConsumptionKwh = annualConsumptionKwh;
        this.pvGenerationKwh = pvGenerationKwh;
        this.batterySizeKwh = batterySizeKwh;
        this.predictedSelfConsumptionPercentage = predictedSelfConsumptionPercentage;
        this.pvToConsumptionRatio = pvToConsumptionRatio;
        this.batteryToConsumptionRatio = batteryToConsumptionRatio;
    }
    
    @Override
    public String toString() {
        return String.format("McsEntry[occupancy=%d, consumption=%.1f, pv=%.1f, battery=%.1f, selfConsumption=%.2f%%]",
                occupancyDays, annualConsumptionKwh, pvGenerationKwh, batterySizeKwh, predictedSelfConsumptionPercentage);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        McsEntry that = (McsEntry) obj;
        return occupancyDays == that.occupancyDays &&
               Double.compare(that.occupancyDaysNormalized, occupancyDaysNormalized) == 0 &&
               Double.compare(that.annualConsumptionKwh, annualConsumptionKwh) == 0 &&
               Double.compare(that.pvGenerationKwh, pvGenerationKwh) == 0 &&
               Double.compare(that.batterySizeKwh, batterySizeKwh) == 0 &&
               Double.compare(that.predictedSelfConsumptionPercentage, predictedSelfConsumptionPercentage) == 0 &&
               Double.compare(that.pvToConsumptionRatio, pvToConsumptionRatio) == 0 &&
               Double.compare(that.batteryToConsumptionRatio, batteryToConsumptionRatio) == 0;
    }
    
    @Override
    public int hashCode() {
        int result = occupancyDays;
        long temp;
        temp = Double.doubleToLongBits(occupancyDaysNormalized);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(annualConsumptionKwh);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(pvGenerationKwh);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(batterySizeKwh);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(predictedSelfConsumptionPercentage);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(pvToConsumptionRatio);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(batteryToConsumptionRatio);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
} 