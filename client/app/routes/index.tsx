// app/routes/index.tsx
import { useState, useEffect, useMemo } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useMutation } from "@tanstack/react-query";
import axiosClient from "@/lib/axios-client";
import { motion, AnimatePresence } from "framer-motion";
import { z } from "zod";
import { zodValidator } from "@tanstack/zod-adapter";
import { ScoreCard } from "@/components/custom/score-card";
import { RoiChart } from "@/components/custom/roi-chart";
import { RoiInputForm } from "@/components/custom/roi-input-form";
import { AffiliateBanner } from "@/components/custom/affiliate-banner";
import { SimplifiedGuide } from "@/components/custom/simplified-guide";
import type { RoiCalculationResponse } from "@/types/roi-api-response-types";
import type {
  SolarRoiCalculatorParams,
  CardinalDirection,
} from "@/types/roi-calculator-types";

// Define default form state values needed for merging
const defaultFormState: SolarRoiCalculatorParams = {
  solarPanelDirection: "south",
  haveOrWillGetEv: false,
  homeOccupancyDuringWorkHours: true,
  needFinance: false,
  batterySize: 10,
  usage: 4500,
  solarSize: 5,
};

// Define Zod schema for the simplified guide search params
const guideSearchParamsSchema = z.object({
  houseSize: z.enum(["small", "medium", "large"]).optional(),
  roofDirection: z
    .enum([
      "north",
      "north-east",
      "north-west",
      "south",
      "south-east",
      "south-west",
      "east",
      "west",
      "dont_know",
    ])
    .optional(),
  hasEv: z.enum(["yes", "no"]).optional(),
  isHome: z.enum(["yes", "no"]).optional(),
  needsFinance: z.enum(["yes", "no"]).optional(),
});

// Infer the type from the schema
type GuideSearchParams = z.infer<typeof guideSearchParamsSchema>;

export const Route = createFileRoute("/")({
  validateSearch: zodValidator(guideSearchParamsSchema),
  component: IndexComponent,
});

const mapGuideParamsToFormState = (
  params: GuideSearchParams
): Partial<SolarRoiCalculatorParams> => {
  const mapping: Partial<SolarRoiCalculatorParams> = {};

  if (params.houseSize) {
    mapping.usage =
      params.houseSize === "small"
        ? 3000
        : params.houseSize === "medium"
          ? 4500
          : 6000;
  }

  if (params.roofDirection) {
    mapping.solarPanelDirection =
      params.roofDirection === "dont_know"
        ? "south"
        : (params.roofDirection as CardinalDirection);
  }

  if (params.hasEv) {
    mapping.haveOrWillGetEv = params.hasEv === "yes";
  }

  if (params.isHome) {
    mapping.homeOccupancyDuringWorkHours = params.isHome === "yes";
  }

  if (params.needsFinance) {
    mapping.needFinance = params.needsFinance === "yes";
  }

  return mapping;
};

// API call function (moved here)
const calculateRoi = async (
  formData: SolarRoiCalculatorParams
): Promise<RoiCalculationResponse> => {
  const { data } = await axiosClient.post<RoiCalculationResponse>(
    "/api/roi/calculate",
    formData
  );
  return data;
};

function IndexComponent() {
  const [results, setResults] = useState<RoiCalculationResponse | null>(null);
  const guideParams = Route.useSearch();
  const navigate = useNavigate(); // For clearing search params later if needed

  // State to control UI: show guide or dashboard
  // Initialize based on whether guide params are present on load
  const [showDashboard, setShowDashboard] = useState(
    () => Object.keys(guideParams).length > 0
  );

  // Calculate initial form data based on search params
  // Use useMemo to avoid recalculating on every render
  const initialFormDataFromParams = useMemo(() => {
    return mapGuideParamsToFormState(guideParams);
  }, [guideParams]);

  // State to hold the *current* data for the main form
  // Initialized with defaults merged with data from params
  const [currentFormData, setCurrentFormData] =
    useState<SolarRoiCalculatorParams>(() => ({
      ...defaultFormState,
      ...initialFormDataFromParams,
    }));

  // Move mutation hook here
  const mutation = useMutation({
    mutationFn: calculateRoi,
    onSuccess: (data) => {
      console.log("Calculation successful:", data);
      setResults(data);
      // Ensure dashboard is shown after successful calculation
      if (!showDashboard) setShowDashboard(true);
    },
    onError: (error) => {
      console.error("Calculation failed:", error);
      // TODO: Show error message to the user
      if (!showDashboard) setShowDashboard(true); // Still show dashboard even on error?
    },
  });

  // Effect to trigger calculation when guide params lead to showing dashboard
  useEffect(() => {
    const hasGuideParams = Object.keys(guideParams).length > 0;
    if (hasGuideParams && !showDashboard) {
      console.log(
        "Guide params present, revealing dashboard and calculating..."
      );
      const formDataToCalculate = {
        ...defaultFormState,
        ...initialFormDataFromParams,
      };
      setCurrentFormData(formDataToCalculate); // Update current form state
      setShowDashboard(true);
      mutation.mutate(formDataToCalculate);
    } else if (!hasGuideParams && showDashboard) {
      // Optional: If params are removed, reset form to defaults?
      // setCurrentFormData(defaultFormState);
      // setResults(null);
    }

    // Handle direct load with params (if showDashboard was true initially)
    if (
      hasGuideParams &&
      showDashboard &&
      !results &&
      !mutation.isPending &&
      !mutation.isError
    ) {
      console.log("Direct load with params, calculating...");
      const formDataToCalculate = {
        ...defaultFormState,
        ...initialFormDataFromParams,
      };
      setCurrentFormData(formDataToCalculate); // Ensure current form state is updated
      mutation.mutate(formDataToCalculate);
    }
  }, [
    guideParams,
    showDashboard,
    initialFormDataFromParams,
    mutation,
    results,
  ]); // Dependencies

  // Handler for when the main form triggers a recalculation
  const handleRecalculate = (updatedFormData: SolarRoiCalculatorParams) => {
    console.log("Recalculating with updated form data:", updatedFormData);
    setCurrentFormData(updatedFormData); // Update the state holding current form data
    mutation.mutate(updatedFormData);
  };

  // Handler for when the simplified guide completes
  const handleGuideComplete = (guideAnswers: Record<string, string>) => {
    console.log("Guide complete, navigating with params:", guideAnswers);
    // Navigate will update guideParams, triggering the useEffect above
    navigate({ to: "/", search: guideAnswers, replace: true });
  };

  const formatCurrency = (amount: number, _currency: string) => {
    return `£${amount.toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  };

  return (
    <div className="container mx-auto p-4 md:p-8">
      <AnimatePresence mode="wait">
        {!showDashboard ? (
          <motion.div
            key="guide"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.5 }}
            className="max-w-2xl mx-auto" // Center the guide
          >
            <SimplifiedGuide onComplete={handleGuideComplete} />
          </motion.div>
        ) : (
          <motion.div
            key="dashboard"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.5, delay: 0.2 }} // Slight delay for smoother feel
          >
            {/* <h1 className="text-4xl md:text-5xl font-extrabold mb-10 pb-2 text-center tracking-tight bg-gradient-to-r from-green-400 to-blue-500 text-transparent bg-clip-text">
              Green Energy Savings Calculator
            </h1> */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Left Column: Inputs, Banner */}
              <div className="lg:col-span-1 space-y-6">
                <RoiInputForm
                  // Pass current form data, not initial
                  formData={currentFormData}
                  onFormDataChange={setCurrentFormData} // Allow form to update parent state directly
                  onCalculate={handleRecalculate} // Pass recalculate handler
                  isCalculating={mutation.isPending} // Pass loading state
                />
                <AffiliateBanner />
                {/* Maybe add a button to go back to the guide? */}
                {/* <Button variant="outline" onClick={() => { navigate({ to: '/', search: {}, replace: true }); setShowDashboard(false); setResults(null); }}>Start Over Guide</Button> */}
              </div>

              {/* Right Column: Results */}
              <div className="lg:col-span-2 space-y-8">
                {/* ScoreCards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-4 xl:grid-cols-4 gap-4">
                  <ScoreCard
                    title="Total System Cost"
                    value={
                      results
                        ? formatCurrency(
                            results.totalCost.amount,
                            results.totalCost.currency
                          )
                        : "--"
                    }
                  />
                  <ScoreCard
                    title="Yearly Savings"
                    value={
                      results
                        ? formatCurrency(
                            results.yearlySavings.amount,
                            results.yearlySavings.currency
                          )
                        : "--"
                    }
                  />
                  <ScoreCard
                    title="Payback Period"
                    value={
                      results
                        ? `${results.paybackPeriod.years < 0 ? ">15" : results.paybackPeriod.years} Years`
                        : "--"
                    }
                  />
                  <ScoreCard
                    title={`ROI (${results ? results.roiPercentage.periodYears : "--"} Years)`}
                    value={
                      results
                        ? `${results.roiPercentage.percentage.toFixed(1)}%`
                        : "--"
                    }
                  />
                </div>

                {/* Chart */}
                <div className="bg-card p-4 md:p-6 rounded-lg shadow-sm">
                  <h2 className="text-xl font-semibold mb-4 text-center">
                    Cumulative Savings Over Time
                  </h2>
                  {mutation.isPending && !results && (
                    <div className="text-center text-muted-foreground py-10">
                      Calculating initial estimate... Please wait.
                    </div>
                  )}
                  {mutation.isError && (
                    <div className="text-center text-red-600 py-10">
                      Calculation failed. Please check inputs or try again
                      later.
                    </div>
                  )}
                  {results ? (
                    <>
                      <RoiChart chartData={results.roiChartData} />
                      {results.roiChartData.breakEvenYear !== null &&
                        results.roiChartData.breakEvenYear > 0 && (
                          <p className="text-sm text-muted-foreground mt-4 text-center">
                            Estimated break-even point around year{" "}
                            {results.roiChartData.breakEvenYear}.
                          </p>
                        )}
                      {results.roiChartData.breakEvenYear !== null &&
                        results.roiChartData.breakEvenYear < 0 && (
                          <p className="text-sm text-muted-foreground mt-4 text-center">
                            Payback period is longer than the system lifespan (
                            {results.roiPercentage.periodYears} years).
                          </p>
                        )}
                    </>
                  ) : (
                    !mutation.isPending &&
                    !mutation.isError && (
                      <div className="text-center text-muted-foreground py-10">
                        Enter your details and click "Calculate ROI" to see the
                        results.
                      </div>
                    )
                  )}
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Footer - only show when dashboard is visible? */}
      {showDashboard && (
        <footer className="mt-12 pt-6 border-t border-border/40">
          <p className="text-center text-sm text-muted-foreground">
            Disclaimer: This calculator provides approximate estimations based
            on standard assumptions and the inputs provided. While we strive for
            accuracy and ease of understanding to aid purchasing decisions,
            these results should be used as a guide only and supplemented with
            more detailed, professional assessments for final decisions.
          </p>
        </footer>
      )}
    </div>
  );
}
