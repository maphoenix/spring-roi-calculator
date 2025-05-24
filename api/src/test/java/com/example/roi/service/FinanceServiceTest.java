package com.example.roi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FinanceServiceTest {

    private FinanceService financeService;

    @BeforeEach
    void setUp() {
        financeService = new FinanceService();
        // Set default interest rate to 5.5% for testing
        ReflectionTestUtils.setField(financeService, "defaultAnnualInterestRate", 0.055);
    }

    @Test
    void testCalculateFinanceBasic() {
        // Test a £10,000 loan over 10 years at 5.5%
        double cost = 10000.0;
        int years = 10;
        
        FinanceService.FinanceCalculationResult result = financeService.calculateFinance(cost, years);
        
        assertEquals(cost, result.getPrincipalAmount(), 0.01);
        assertEquals(years, result.getLoanTermYears());
        assertEquals(0.055, result.getAnnualInterestRate(), 0.001);
        
        // Monthly payment should be around £108-110 for this loan
        assertTrue(result.getMonthlyPayment() > 100 && result.getMonthlyPayment() < 120);
        
        // Total cost should be more than principal
        assertTrue(result.getTotalCost() > cost);
        
        // Total interest should be positive
        assertTrue(result.getTotalInterest() > 0);
        
        // Verify total = principal + interest
        assertEquals(result.getTotalCost(), 
                     result.getPrincipalAmount() + result.getTotalInterest(), 0.01);
    }

    @Test
    void testCalculateFinanceWithCustomRate() {
        double cost = 5000.0;
        int years = 5;
        double customRate = 0.03; // 3%
        
        FinanceService.FinanceCalculationResult result = 
            financeService.calculateFinance(cost, years, customRate);
        
        assertEquals(customRate, result.getAnnualInterestRate(), 0.001);
        assertTrue(result.getTotalInterest() > 0);
        assertTrue(result.getTotalCost() > cost);
    }

    @Test
    void testCalculateFinanceZeroInterest() {
        double cost = 1000.0;
        int years = 2;
        double zeroRate = 0.0;
        
        FinanceService.FinanceCalculationResult result = 
            financeService.calculateFinance(cost, years, zeroRate);
        
        assertEquals(0.0, result.getAnnualInterestRate());
        assertEquals(0.0, result.getTotalInterest());
        assertEquals(cost, result.getTotalCost());
        assertEquals(cost / (years * 12), result.getMonthlyPayment(), 0.01);
    }

    @Test
    void testCalculateFinanceInvalidInputs() {
        // Test negative cost
        assertThrows(IllegalArgumentException.class, 
                     () -> financeService.calculateFinance(-1000, 5));
        
        // Test zero cost
        assertThrows(IllegalArgumentException.class, 
                     () -> financeService.calculateFinance(0, 5));
        
        // Test negative years
        assertThrows(IllegalArgumentException.class, 
                     () -> financeService.calculateFinance(1000, -5));
        
        // Test zero years
        assertThrows(IllegalArgumentException.class, 
                     () -> financeService.calculateFinance(1000, 0));
        
        // Test negative interest rate
        assertThrows(IllegalArgumentException.class, 
                     () -> financeService.calculateFinance(1000, 5, -0.01));
    }

    @Test
    void testCompareFinanceVsCash() {
        double cost = 8000.0;
        int years = 6;
        
        String comparison = financeService.compareFinanceVsCash(cost, years);
        
        // Should contain key information
        assertTrue(comparison.contains("Cash purchase"));
        assertTrue(comparison.contains("Financed"));
        assertTrue(comparison.contains("Extra cost"));
        assertTrue(comparison.contains("£"));
    }

    @Test
    void testMonthlyPaymentCalculation() {
        // Test known values - £10k at 6% for 5 years should be around £193/month
        double cost = 10000.0;
        int years = 5;
        double rate = 0.06; // 6%
        
        FinanceService.FinanceCalculationResult result = 
            financeService.calculateFinance(cost, years, rate);
        
        // Monthly payment should be around £193 for this specific scenario
        assertTrue(result.getMonthlyPayment() > 190 && result.getMonthlyPayment() < 200,
                   "Monthly payment was: " + result.getMonthlyPayment());
    }

    @Test
    void testGetDefaultInterestRate() {
        double defaultRate = financeService.getDefaultInterestRate();
        assertEquals(0.055, defaultRate, 0.001);
    }
} 