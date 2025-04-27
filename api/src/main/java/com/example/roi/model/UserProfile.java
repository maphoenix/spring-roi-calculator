package com.example.roi.model;

public class UserProfile {
    
    public enum HouseSize {
        SMALL, // < 1000 sq ft
        MEDIUM, // 1000-2000 sq ft
        LARGE // > 2000 sq ft
    }
    
    public enum TaxBracket {
        TWENTY_PERCENT, // 20% tax rate
        FORTY_PERCENT, // 40% tax rate
        PREFER_NOT_TO_SAY // No tax information provided
    }
    
    private HouseSize houseSize;
    private boolean hasOrPlanningEv;
    private TaxBracket taxBracket;
    private boolean homeOccupiedDuringDay;
    private boolean needsFinancing;
    
    // Default constructor
    public UserProfile() {
        this.houseSize = HouseSize.MEDIUM;
        this.hasOrPlanningEv = false;
        this.taxBracket = TaxBracket.TWENTY_PERCENT;
        this.homeOccupiedDuringDay = false;
        this.needsFinancing = false;
    }
    
    // Getters and setters
    public HouseSize getHouseSize() {
        return houseSize;
    }
    
    public void setHouseSize(HouseSize houseSize) {
        this.houseSize = houseSize;
    }
    
    public boolean isHasOrPlanningEv() {
        return hasOrPlanningEv;
    }
    
    public void setHasOrPlanningEv(boolean hasOrPlanningEv) {
        this.hasOrPlanningEv = hasOrPlanningEv;
    }
    
    public TaxBracket getTaxBracket() {
        return taxBracket;
    }
    
    public void setTaxBracket(TaxBracket taxBracket) {
        this.taxBracket = taxBracket;
    }
    
    public boolean isHomeOccupiedDuringDay() {
        return homeOccupiedDuringDay;
    }
    
    public void setHomeOccupiedDuringDay(boolean homeOccupiedDuringDay) {
        this.homeOccupiedDuringDay = homeOccupiedDuringDay;
    }
    
    public boolean isNeedsFinancing() {
        return needsFinancing;
    }
    
    public void setNeedsFinancing(boolean needsFinancing) {
        this.needsFinancing = needsFinancing;
    }
} 