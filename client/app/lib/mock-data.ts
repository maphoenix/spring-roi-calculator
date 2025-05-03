import {
  MonthlySavings,
  YearlySavings,
  PaybackPeriod,
  RoiChartData,
  RoiPercentage,
  TotalCost,
} from "@/types/roi-response-types";

export const mockMonthlySavings: MonthlySavings = {
  amount: 150,
  currency: "GBP",
};

export const mockYearlySavings: YearlySavings = {
  amount: 1800,
  currency: "GBP",
};

export const mockPaybackPeriod: PaybackPeriod = {
  years: 7.5,
};

export const mockRoiChartData: RoiChartData = {
  dataPoints: [
    { year: 0, cumulativeSavings: -10000, currency: "GBP" }, // Initial Investment
    { year: 1, cumulativeSavings: -8200, currency: "GBP" },
    { year: 2, cumulativeSavings: -6400, currency: "GBP" },
    { year: 3, cumulativeSavings: -4600, currency: "GBP" },
    { year: 4, cumulativeSavings: -2800, currency: "GBP" },
    { year: 5, cumulativeSavings: -1000, currency: "GBP" },
    { year: 6, cumulativeSavings: 800, currency: "GBP" },
    { year: 7, cumulativeSavings: 2600, currency: "GBP" },
    { year: 8, cumulativeSavings: 4400, currency: "GBP" }, // Breakeven around here
    { year: 9, cumulativeSavings: 6200, currency: "GBP" },
    { year: 10, cumulativeSavings: 8000, currency: "GBP" },
    { year: 11, cumulativeSavings: 9800, currency: "GBP" },
    { year: 12, cumulativeSavings: 11600, currency: "GBP" },
    { year: 13, cumulativeSavings: 13400, currency: "GBP" },
    { year: 14, cumulativeSavings: 15200, currency: "GBP" },
    { year: 15, cumulativeSavings: 17000, currency: "GBP" },
  ],
  breakEvenYear: 8, // Example break-even year
};

export const mockRoiPercentage: RoiPercentage = {
  percentage: 70, // Example: 70% ROI over 15 years
  periodYears: 15,
};

export const mockTotalCost: TotalCost = {
  // Derived from mockRoiChartData year 0
  amount: Math.abs(mockRoiChartData.dataPoints[0].cumulativeSavings),
  currency: mockRoiChartData.dataPoints[0].currency,
};
