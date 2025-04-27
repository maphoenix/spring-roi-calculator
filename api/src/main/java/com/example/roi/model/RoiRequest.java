package com.example.roi.model;

public class RoiRequest {
    private double batterySize = 17.5;
    private double usage = 4000;
    private double solarSize = 4.0;
    
    public RoiRequest() {
        // Empty constructor
    }
    
    public double getBatterySize() {
        return batterySize;
    }
    
    public void setBatterySize(double batterySize) {
        this.batterySize = batterySize;
    }
    
    public double getUsage() {
        return usage;
    }
    
    public void setUsage(double usage) {
        this.usage = usage;
    }
    
    public double getSolarSize() {
        return solarSize;
    }
    
    public void setSolarSize(double solarSize) {
        this.solarSize = solarSize;
    }
}
