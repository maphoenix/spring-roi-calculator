package com.example.roi.model;

public class MonthlySavings {

    private double amount;
    private String currency = "GBP";

    public MonthlySavings(double amount) {
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
