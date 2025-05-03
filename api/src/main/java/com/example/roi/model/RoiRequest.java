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
    private boolean homeOccupancyDuringWorkHours;
    private boolean needFinance;
    private double batterySize = 17.5;
    private double usage = 4000;
    private double solarSize = 4.0;

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
    public boolean isHomeOccupancyDuringWorkHours() {
        return homeOccupancyDuringWorkHours;
    }

    public void setHomeOccupancyDuringWorkHours(boolean homeOccupancyDuringWorkHours) {
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
}
