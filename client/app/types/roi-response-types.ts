/**
 * Represents the monthly savings from the solar installation.
 */
export interface MonthlySavings {
  /**
   * The estimated monthly savings amount.
   */
  amount: number;
  /**
   * The currency of the savings amount.
   */
  currency: string;
}

/**
 * Represents the yearly savings from the solar installation.
 */
export interface YearlySavings {
  /**
   * The estimated yearly savings amount.
   */
  amount: number;
  /**
   * The currency of the savings amount.
   */
  currency: string;
}

/**
 * Represents the payback period for the solar investment.
 */
export interface PaybackPeriod {
  /**
   * The estimated payback period in years.
   */
  years: number;
}

/**
 * Represents a single data point for the ROI chart.
 */
export interface RoiChartDataPoint {
  /**
   * The year for this data point.
   */
  year: number;
  /**
   * The cumulative savings (positive) or cost (negative) up to this year.
   */
  cumulativeSavings: number;
  /**
   * The currency of the savings amount.
   */
  currency: string;
}

/**
 * Represents the data needed for the ROI line chart.
 */
export interface RoiChartData {
  /**
   * An array of data points for the chart.
   */
  dataPoints: RoiChartDataPoint[];
  /**
   * The year in which the cumulative savings break even (become positive).
   */
  breakEvenYear: number | null;
}

/**
 * Represents the estimated ROI percentage over a defined period.
 */
export interface RoiPercentage {
  /**
   * The estimated ROI percentage.
   */
  percentage: number;
  /**
   * The period over which the ROI is calculated (in years).
   */
  periodYears: number;
}

/**
 * Represents the total initial cost of the installation.
 */
export interface TotalCost {
  /**
   * The total cost amount.
   */
  amount: number;
  /**
   * The currency of the cost amount.
   */
  currency: string;
}
