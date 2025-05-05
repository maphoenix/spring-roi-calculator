import axiosClient from "@/lib/axios-client";
import { RoiCalculationResponse } from "@/types/roi-api-response-types";
import { SolarRoiCalculatorParams } from "@/types/roi-calculator-types";

/**
 * Calculates the Return on Investment (ROI) for solar panel installation
 * based on the provided parameters.
 *
 * @param {SolarRoiCalculatorParams} formData - The form data containing parameters
 * such as solar size, battery size, usage, panel direction, EV ownership,
 * home occupancy, and financing needs.
 *
 * @returns {Promise<RoiCalculationResponse>} A promise that resolves to the ROI calculation
 * response containing financial projections and savings information.
 *
 * @throws Will throw an error if the API request fails.
 */
export const calculateRoi = async (
  formData: SolarRoiCalculatorParams
): Promise<RoiCalculationResponse> => {
  const { data } = await axiosClient.post<RoiCalculationResponse>(
    "/api/roi/calculate",
    formData
  );
  return data;
};
