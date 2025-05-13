package com.example.roi.model;

public class TotalCost {

    private double amount;
    private String currency = "GBP"; // Defaulting to GBP

    public TotalCost() {}

    public TotalCost(double amount) {
        this.amount = amount;
    }

    // Getters
    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}