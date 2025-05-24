package com.example.roi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for calculating finance costs for solar and battery installations.
 * Handles loan repayment calculations including monthly payments, total cost,
 * and interest calculations over the loan term.
 * 
 * Now integrated with InterestRateService to use current market rates.
 */
@Service
public class FinanceService {

    private static final Logger logger = LoggerFactory.getLogger(FinanceService.class);

    @Autowired
    private InterestRateService interestRateService;

    // Default annual interest rate (can be overridden via application properties)
    @Value("${finance.default.annual.rate:0.055}")
    private double defaultAnnualInterestRate;

    /**
     * Represents the result of a finance calculation.
     */
    public static class FinanceCalculationResult {
        private final double principalAmount;
        private final int loanTermYears;
        private final double annualInterestRate;
        private final double monthlyPayment;
        private final double totalCost;
        private final double totalInterest;
        private final double monthlyCostVsCash;
        private final String rateSource;

        public FinanceCalculationResult(double principalAmount, int loanTermYears, 
                                      double annualInterestRate, double monthlyPayment, 
                                      double totalCost, double totalInterest, 
                                      double monthlyCostVsCash, String rateSource) {
            this.principalAmount = principalAmount;
            this.loanTermYears = loanTermYears;
            this.annualInterestRate = annualInterestRate;
            this.monthlyPayment = monthlyPayment;
            this.totalCost = totalCost;
            this.totalInterest = totalInterest;
            this.monthlyCostVsCash = monthlyCostVsCash;
            this.rateSource = rateSource;
        }

        // Getters
        public double getPrincipalAmount() { return principalAmount; }
        public int getLoanTermYears() { return loanTermYears; }
        public double getAnnualInterestRate() { return annualInterestRate; }
        public double getMonthlyPayment() { return monthlyPayment; }
        public double getTotalCost() { return totalCost; }
        public double getTotalInterest() { return totalInterest; }
        public double getMonthlyCostVsCash() { return monthlyCostVsCash; }
        public String getRateSource() { return rateSource; }

        @Override
        public String toString() {
            return String.format(
                "FinanceCalculation[principal=£%.2f, term=%d years, rate=%.2f%%, monthly=£%.2f, total=£%.2f, interest=£%.2f, source=%s]",
                principalAmount, loanTermYears, annualInterestRate * 100, 
                monthlyPayment, totalCost, totalInterest, rateSource
            );
        }
    }

    /**
     * Calculate finance costs using current market rates.
     *
     * @param cost The principal amount to finance
     * @param years The loan term in years
     * @return FinanceCalculationResult containing all calculated values
     */
    public FinanceCalculationResult calculateFinance(double cost, int years) {
        double marketRate = interestRateService.getBestRateForLoan(years, cost);
        return calculateFinance(cost, years, marketRate, "Current Market Rate");
    }

    /**
     * Calculate finance costs using green energy rates (typically lower).
     *
     * @param cost The principal amount to finance
     * @param years The loan term in years
     * @return FinanceCalculationResult containing all calculated values
     */
    public FinanceCalculationResult calculateGreenEnergyFinance(double cost, int years) {
        double greenRate = interestRateService.getGreenEnergyRate();
        return calculateFinance(cost, years, greenRate, "Green Energy Rate");
    }

    /**
     * Calculate finance costs with a custom interest rate.
     *
     * @param cost The principal amount to finance
     * @param years The loan term in years  
     * @param annualInterestRate The annual interest rate (as decimal, e.g., 0.055 for 5.5%)
     * @return FinanceCalculationResult containing all calculated values
     */
    public FinanceCalculationResult calculateFinance(double cost, int years, double annualInterestRate) {
        return calculateFinance(cost, years, annualInterestRate, "Custom Rate");
    }

    /**
     * Internal method to calculate finance costs with rate source tracking.
     */
    private FinanceCalculationResult calculateFinance(double cost, int years, double annualInterestRate, String source) {
        // Validate inputs
        if (cost <= 0) {
            throw new IllegalArgumentException("Cost must be greater than 0");
        }
        if (years <= 0) {
            throw new IllegalArgumentException("Loan term must be greater than 0 years");
        }
        if (annualInterestRate < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }

        // Special case: if interest rate is 0, it's essentially cash payment
        if (annualInterestRate == 0.0) {
            return new FinanceCalculationResult(
                cost, years, annualInterestRate, 
                cost / (years * 12), // Simple division over months
                cost, 0.0, 0.0, source
            );
        }

        // Calculate monthly interest rate and number of payments
        double monthlyInterestRate = annualInterestRate / 12.0;
        int numberOfPayments = years * 12;

        // Calculate monthly payment using the standard loan payment formula
        // M = P * [r(1+r)^n] / [(1+r)^n - 1]
        double monthlyPayment = calculateMonthlyPayment(cost, monthlyInterestRate, numberOfPayments);
        
        // Calculate total cost and interest
        double totalCost = monthlyPayment * numberOfPayments;
        double totalInterest = totalCost - cost;
        
        // Calculate additional monthly cost vs paying cash
        double monthlyCostVsCash = totalInterest / (years * 12);

        FinanceCalculationResult result = new FinanceCalculationResult(
            cost, years, annualInterestRate, monthlyPayment, 
            totalCost, totalInterest, monthlyCostVsCash, source
        );

        logger.info("Finance calculation: {}", result);
        
        return result;
    }

    /**
     * Calculate monthly payment using the standard loan payment formula.
     */
    private double calculateMonthlyPayment(double principal, double monthlyRate, int numPayments) {
        if (monthlyRate == 0.0) {
            return principal / numPayments;
        }
        
        double factor = Math.pow(1 + monthlyRate, numPayments);
        return principal * (monthlyRate * factor) / (factor - 1);
    }

    /**
     * Compare financing vs cash purchase using current market rates.
     *
     * @param cost The purchase cost
     * @param years The loan term
     * @return A comparison result showing the difference
     */
    public String compareFinanceVsCash(double cost, int years) {
        FinanceCalculationResult financeResult = calculateFinance(cost, years);
        
        return String.format(
            "Cash purchase: £%.2f | Financed: £%.2f total (£%.2f/month) | Extra cost: £%.2f (£%.2f/month) | Rate: %.2f%% (%s)",
            cost,
            financeResult.getTotalCost(),
            financeResult.getMonthlyPayment(),
            financeResult.getTotalInterest(),
            financeResult.getMonthlyCostVsCash(),
            financeResult.getAnnualInterestRate() * 100,
            financeResult.getRateSource()
        );
    }

    /**
     * Compare different financing options (standard vs green energy).
     *
     * @param cost The purchase cost
     * @param years The loan term
     * @return Comparison of standard vs green energy financing
     */
    public String compareFinancingOptions(double cost, int years) {
        FinanceCalculationResult standard = calculateFinance(cost, years);
        FinanceCalculationResult green = calculateGreenEnergyFinance(cost, years);
        
        double savings = standard.getTotalCost() - green.getTotalCost();
        double monthlySavings = standard.getMonthlyPayment() - green.getMonthlyPayment();
        
        return String.format(
            "Standard Loan: £%.2f/month (%.2f%% APR) | Green Energy Loan: £%.2f/month (%.2f%% APR) | " +
            "Savings with green loan: £%.2f total (£%.2f/month)",
            standard.getMonthlyPayment(), standard.getAnnualInterestRate() * 100,
            green.getMonthlyPayment(), green.getAnnualInterestRate() * 100,
            savings, monthlySavings
        );
    }

    /**
     * Get current market rates summary.
     */
    public String getCurrentRatesSummary() {
        return interestRateService.getRateSummary();
    }

    /**
     * Get the current default interest rate.
     */
    public double getDefaultInterestRate() {
        return defaultAnnualInterestRate;
    }

    /**
     * Refresh interest rates from external sources.
     */
    public void refreshMarketRates() {
        interestRateService.refreshRates();
    }
} 