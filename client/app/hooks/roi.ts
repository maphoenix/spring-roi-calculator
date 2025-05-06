import { useMutation, UseMutationResult } from "@tanstack/react-query";
import { calculateRoi } from "@/crud/roi";
import { RoiCalculationResponse } from "@/types/roi-api-response-types";
import { SolarRoiCalculatorParams } from "@/types/roi-calculator-types";

/**
 * Custom hook for handling ROI calculations with React Query
 *
 * This hook creates a mutation for calculating the Return on Investment (ROI) for solar panel installations.
 * It manages the loading states, results, and dashboard visibility throughout the calculation process.
 *
 * @param {function} setIsDebouncing - Function to update the debouncing state
 * @param {function} setResults - Function to update the calculation results
 * @param {boolean} showDashboard - Current state of dashboard visibility
 * @param {function} setShowDashboard - Function to update dashboard visibility
 *
 * @returns {UseMutationResult} A React Query mutation object for ROI calculations
 */
export const useRoi = (
  setIsDebouncing: (isDebouncing: boolean) => void,
  setResults: (results: RoiCalculationResponse | null) => void,
  showDashboard: boolean,
  setShowDashboard: (showDashboard: boolean) => void
): UseMutationResult<
  RoiCalculationResponse,
  Error,
  SolarRoiCalculatorParams
> => {
  return useMutation({
    mutationFn: calculateRoi,
    onMutate: () => {
      console.log("Mutation starting (onMutate)... calculation beginning.");
      // Hide debounce indicator *just as* the actual API call starts
      setIsDebouncing(false);
    },
    onSuccess: (data) => {
      console.log("Calculation successful:", data);
      setResults(data);
      // Ensure cleared on success (might be redundant if onMutate fired)
      setIsDebouncing(false);
      if (!showDashboard) setShowDashboard(true);
    },
    onError: (error) => {
      console.error("Calculation failed:", error);
      setResults(null);
      // Ensure cleared on error
      setIsDebouncing(false);
      if (!showDashboard) setShowDashboard(true); // Still show dashboard on error
    },
    onSettled: () => {
      // Final cleanup ensure debouncing indicator is off
      console.log("Mutation settled.");
      setIsDebouncing(false);
    },
  });
};
