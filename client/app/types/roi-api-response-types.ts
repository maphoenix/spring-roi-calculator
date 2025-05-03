// Corresponds to the Java RoiCalculationResponse DTO

import type {
  TotalCost,
  YearlySavings,
  MonthlySavings,
  PaybackPeriod,
  RoiChartData,
  RoiPercentage,
} from "./roi-response-types"; // Assuming existing types are in this file

export interface RoiCalculationResponse {
  totalCost: TotalCost;
  yearlySavings: YearlySavings;
  monthlySavings: MonthlySavings;
  paybackPeriod: PaybackPeriod;
  roiChartData: RoiChartData;
  roiPercentage: RoiPercentage;
}
