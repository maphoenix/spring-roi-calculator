package com.example.roi.model;

public class YearlySavings {

    private double amount;
    private String currency = "GBP";

    public YearlySavings() {}

    public YearlySavings(double amount) {
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
