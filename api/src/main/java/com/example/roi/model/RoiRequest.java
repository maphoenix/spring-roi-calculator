package com.example.roi.model;

import java.util.ArrayList;
import java.util.List;

public class RoiRequest {
    private double batterySize = 17.5;
    private double usage = 4000;
    private double solarSize = 4.0;
    private List<Tariff> tariffs = new ArrayList<>();
    
    public RoiRequest() {
        // Empty constructor - tariffs will be populated by the service
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
    
    public List<Tariff> getTariffs() {
        return tariffs;
    }
    
    public void setTariffs(List<Tariff> tariffs) {
        this.tariffs = tariffs;
    }
}
