package com.example.roi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoiRequest {

    public enum CardinalDirection {
        @JsonProperty("north")
        NORTH,
        @JsonProperty("north-east")
        NORTH_EAST,
        @JsonProperty("north-west")
        NORTH_WEST,
        @JsonProperty("south")
        SOUTH,
        @JsonProperty("south-east")
        SOUTH_EAST,
        @JsonProperty("south-west")
        SOUTH_WEST,
        @JsonProperty("east")
        EAST,
        @JsonProperty("west")
        WEST
    }

    private CardinalDirection solarPanelDirection;
    private boolean haveOrWillGetEv;
    private int homeOccupancyDuringWorkHours = 5; // Default to 5 (home all day)
    private boolean needFinance;
    private double batterySize = 17.5;
    private double usage = 4000;
    private double solarSize = 4.0;
    private boolean includePdfBreakdown = false;

    public RoiRequest() {
        // Empty constructor
    }

    public CardinalDirection getSolarPanelDirection() {
        return solarPanelDirection;
    }

    public void setSolarPanelDirection(CardinalDirection solarPanelDirection) {
        this.solarPanelDirection = solarPanelDirection;
    }

    @JsonProperty("haveOrWillGetEv")
    public boolean isHaveOrWillGetEv() {
        return haveOrWillGetEv;
    }

    public void setHaveOrWillGetEv(boolean haveOrWillGetEv) {
        this.haveOrWillGetEv = haveOrWillGetEv;
    }

    @JsonProperty("homeOccupancyDuringWorkHours")
    public int getHomeOccupancyDuringWorkHours() {
        return homeOccupancyDuringWorkHours;
    }

    public void setHomeOccupancyDuringWorkHours(int homeOccupancyDuringWorkHours) {
        if (homeOccupancyDuringWorkHours < 1 || homeOccupancyDuringWorkHours > 5) {
            throw new IllegalArgumentException("Home occupancy during work hours must be between 1 and 5 (days), got: " + homeOccupancyDuringWorkHours);
        }
        this.homeOccupancyDuringWorkHours = homeOccupancyDuringWorkHours;
    }

    @JsonProperty("needFinance")
    public boolean isNeedFinance() {
        return needFinance;
    }

    public void setNeedFinance(boolean needFinance) {
        this.needFinance = needFinance;
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

    @JsonProperty("includePdfBreakdown")
    public boolean isIncludePdfBreakdown() {
        return includePdfBreakdown;
    }

    public void setIncludePdfBreakdown(boolean includePdfBreakdown) {
        this.includePdfBreakdown = includePdfBreakdown;
    }

    @Override
    public String toString() {
        return "RoiRequest{" +
                "solarPanelDirection=" + getSolarPanelDirection() +
                ", haveOrWillGetEv=" + isHaveOrWillGetEv() +
                ", homeOccupancyDuringWorkHours=" + getHomeOccupancyDuringWorkHours() +
                ", needFinance=" + isNeedFinance() +
                ", solarSize=" + getSolarSize() +
                ", batterySize=" + getBatterySize() +
                ", usage=" + getUsage() +
                ", includePdfBreakdown=" + isIncludePdfBreakdown();
    }

}
