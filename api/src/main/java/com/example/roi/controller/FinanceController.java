package com.example.roi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.roi.service.FinanceService;
import com.example.roi.service.InterestRateService;

/**
 * REST controller for finance calculations.
 * Provides endpoints for calculating loan payments and comparing financing options.
 * Now includes current market rates and green energy financing options.
 */
@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    @Autowired
    private InterestRateService interestRateService;

    /**
     * Request object for finance calculations.
     */
    public static class FinanceRequest {
        private double cost;
        private int years;
        private Double customInterestRate; // Optional custom rate
        private boolean useGreenEnergyRate = false; // Use special green energy rates

        public FinanceRequest() {}

        public FinanceRequest(double cost, int years) {
            this.cost = cost;
            this.years = years;
        }

        public FinanceRequest(double cost, int years, double customInterestRate) {
            this.cost = cost;
            this.years = years;
            this.customInterestRate = customInterestRate;
        }

        // Getters and setters
        public double getCost() { return cost; }
        public void setCost(double cost) { this.cost = cost; }
        
        public int getYears() { return years; }
        public void setYears(int years) { this.years = years; }
        
        public Double getCustomInterestRate() { return customInterestRate; }
        public void setCustomInterestRate(Double customInterestRate) { 
            this.customInterestRate = customInterestRate; 
        }

        public boolean isUseGreenEnergyRate() { return useGreenEnergyRate; }
        public void setUseGreenEnergyRate(boolean useGreenEnergyRate) { 
            this.useGreenEnergyRate = useGreenEnergyRate; 
        }
    }

    /**
     * Calculate finance costs using GET request with query parameters.
     * Uses current market rates by default.
     * 
     * @param cost The amount to finance
     * @param years The loan term in years
     * @param rate Optional custom interest rate (defaults to current market rate)
     * @param green Whether to use green energy rates (defaults to false)
     * @return Finance calculation result
     */
    @GetMapping("/calculate")
    public ResponseEntity<FinanceService.FinanceCalculationResult> calculateFinance(
            @RequestParam double cost,
            @RequestParam int years,
            @RequestParam(required = false) Double rate,
            @RequestParam(required = false, defaultValue = "false") boolean green) {
        
        try {
            FinanceService.FinanceCalculationResult result;
            
            if (rate != null) {
                result = financeService.calculateFinance(cost, years, rate);
            } else if (green) {
                result = financeService.calculateGreenEnergyFinance(cost, years);
            } else {
                result = financeService.calculateFinance(cost, years);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Calculate finance costs using POST request with JSON body.
     * 
     * @param request Finance calculation request
     * @return Finance calculation result
     */
    @PostMapping("/calculate")
    public ResponseEntity<FinanceService.FinanceCalculationResult> calculateFinance(
            @RequestBody FinanceRequest request) {
        
        try {
            FinanceService.FinanceCalculationResult result;
            
            if (request.getCustomInterestRate() != null) {
                result = financeService.calculateFinance(
                    request.getCost(), 
                    request.getYears(), 
                    request.getCustomInterestRate()
                );
            } else if (request.isUseGreenEnergyRate()) {
                result = financeService.calculateGreenEnergyFinance(
                    request.getCost(), 
                    request.getYears()
                );
            } else {
                result = financeService.calculateFinance(request.getCost(), request.getYears());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Compare financing vs cash purchase using current market rates.
     * 
     * @param cost The amount to finance
     * @param years The loan term in years
     * @return Comparison string
     */
    @GetMapping("/compare")
    public ResponseEntity<String> compareFinanceVsCash(
            @RequestParam double cost,
            @RequestParam int years) {
        
        try {
            String comparison = financeService.compareFinanceVsCash(cost, years);
            return ResponseEntity.ok(comparison);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid parameters");
        }
    }

    /**
     * Compare standard financing vs green energy financing.
     * 
     * @param cost The amount to finance
     * @param years The loan term in years
     * @return Comparison between standard and green energy rates
     */
    @GetMapping("/compare-options")
    public ResponseEntity<String> compareFinancingOptions(
            @RequestParam double cost,
            @RequestParam int years) {
        
        try {
            String comparison = financeService.compareFinancingOptions(cost, years);
            return ResponseEntity.ok(comparison);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid parameters");
        }
    }

    /**
     * Get current market interest rates summary.
     * 
     * @return Current rates information
     */
    @GetMapping("/current-rates")
    public ResponseEntity<String> getCurrentRates() {
        String ratesSummary = financeService.getCurrentRatesSummary();
        return ResponseEntity.ok(ratesSummary);
    }

    /**
     * Get detailed current market rates data.
     * 
     * @return Current rates data object
     */
    @GetMapping("/rates-data")
    public ResponseEntity<InterestRateService.InterestRateData> getCurrentRatesData() {
        InterestRateService.InterestRateData rates = interestRateService.getCurrentRates();
        return ResponseEntity.ok(rates);
    }

    /**
     * Force refresh of interest rates from external sources.
     * 
     * @return Updated rates information
     */
    @PostMapping("/refresh-rates")
    public ResponseEntity<String> refreshRates() {
        try {
            financeService.refreshMarketRates();
            String ratesSummary = financeService.getCurrentRatesSummary();
            return ResponseEntity.ok("Rates refreshed successfully:\n" + ratesSummary);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Failed to refresh rates: " + e.getMessage());
        }
    }

    /**
     * Get the current default interest rate.
     * 
     * @return Default interest rate
     */
    @GetMapping("/default-rate")
    public ResponseEntity<Double> getDefaultInterestRate() {
        double rate = financeService.getDefaultInterestRate();
        return ResponseEntity.ok(rate);
    }

    /**
     * Get the best available rate for a specific loan amount and term.
     * 
     * @param cost The loan amount
     * @param years The loan term
     * @return Best available rate
     */
    @GetMapping("/best-rate")
    public ResponseEntity<Double> getBestRate(
            @RequestParam double cost,
            @RequestParam int years) {
        
        try {
            double bestRate = interestRateService.getBestRateForLoan(years, cost);
            return ResponseEntity.ok(bestRate);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get the green energy financing rate.
     * 
     * @return Green energy loan rate
     */
    @GetMapping("/green-rate")
    public ResponseEntity<Double> getGreenEnergyRate() {
        double rate = interestRateService.getGreenEnergyRate();
        return ResponseEntity.ok(rate);
    }
} 