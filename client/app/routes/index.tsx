// app/routes/index.tsx
import { useState, useEffect, useMemo, useCallback } from "react";
import {
  createFileRoute,
  useNavigate,
  useSearch,
} from "@tanstack/react-router";
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
import { Button } from "@/components/ui/button";
import { Share2Icon } from "lucide-react";
import { useDebouncedCallback } from "use-debounce";

// Define Zod schema for ALL possible search params (guide + main form)
const combinedSearchParamsSchema = z.object({
  // Guide params
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
  // Main form params (use zod transforms for types)
  batterySize: z.coerce.number().min(0).optional(),
  solarSize: z.coerce.number().min(0).optional(),
  usage: z.coerce.number().min(100).optional(),
  solarPanelDirection: z
    .enum([
      "north",
      "north_east", // Ensure these match CardinalDirection or map them
      "north_west",
      "south",
      "south_east",
      "south_west",
      "east",
      "west",
    ])
    .optional(),
  haveOrWillGetEv: z.coerce.boolean().optional(),
  homeOccupancyDuringWorkHours: z.coerce.boolean().optional(),
  needFinance: z.coerce.boolean().optional(), // Can overlap with guide param if needed
});

// Infer the type from the schema
type CombinedSearchParams = z.infer<typeof combinedSearchParamsSchema>;

// Define default form state values
const defaultFormState: SolarRoiCalculatorParams = {
  solarPanelDirection: "south",
  haveOrWillGetEv: false,
  homeOccupancyDuringWorkHours: true,
  needFinance: false,
  batterySize: 10,
  usage: 4500,
  solarSize: 5,
};

export const Route = createFileRoute("/")({
  validateSearch: zodValidator(combinedSearchParamsSchema),
  component: IndexComponent,
});

// --- Mapping Functions (Simplified/Adjusted) ---

const mapGuideParamsToFormState = (
  params: CombinedSearchParams
): Partial<SolarRoiCalculatorParams> => {
  const mapping: Partial<SolarRoiCalculatorParams> = {};

  // Map only guide-specific params that *don't* directly overlap
  // with main form params if both might exist
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
        : (params.roofDirection.replace("-", "_") as CardinalDirection); // Adjust format
  }
  if (params.hasEv) mapping.haveOrWillGetEv = params.hasEv === "yes";
  if (params.isHome)
    mapping.homeOccupancyDuringWorkHours = params.isHome === "yes";
  if (params.needsFinance) mapping.needFinance = params.needsFinance === "yes"; // Guide version

  return mapping;
};

// Function to get form state directly from combined search params
const getFormStateFromCombinedParams = (
  params: CombinedSearchParams
): Partial<SolarRoiCalculatorParams> => {
  const formState: Partial<SolarRoiCalculatorParams> = {};

  if (params.batterySize !== undefined)
    formState.batterySize = params.batterySize;
  if (params.solarSize !== undefined) formState.solarSize = params.solarSize;
  if (params.usage !== undefined) formState.usage = params.usage;
  if (params.solarPanelDirection !== undefined)
    formState.solarPanelDirection =
      params.solarPanelDirection as CardinalDirection;
  if (params.haveOrWillGetEv !== undefined)
    formState.haveOrWillGetEv = params.haveOrWillGetEv;
  if (params.homeOccupancyDuringWorkHours !== undefined)
    formState.homeOccupancyDuringWorkHours =
      params.homeOccupancyDuringWorkHours;
  if (params.needFinance !== undefined)
    formState.needFinance = params.needFinance; // Main form version

  return formState;
};

// API call function
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
  const searchParams = Route.useSearch(); // Use the validated/typed search params
  const navigate = useNavigate();

  // Determine initial state based on params
  const getInitialState = useCallback(() => {
    const hasGuideParams =
      searchParams.houseSize ||
      searchParams.roofDirection ||
      searchParams.hasEv ||
      searchParams.isHome ||
      searchParams.needsFinance; // Check for *any* guide-specific param

    const hasMainFormParams =
      searchParams.batterySize !== undefined ||
      searchParams.solarSize !== undefined ||
      searchParams.usage !== undefined ||
      searchParams.solarPanelDirection !== undefined ||
      searchParams.haveOrWillGetEv !== undefined ||
      searchParams.homeOccupancyDuringWorkHours !== undefined ||
      searchParams.needFinance !== undefined; // Check for *any* main form param

    const showDashboardInitially = hasGuideParams || hasMainFormParams;

    let initialFormData: SolarRoiCalculatorParams;
    if (hasMainFormParams) {
      // Prioritize direct main form params if they exist
      initialFormData = {
        ...defaultFormState,
        ...getFormStateFromCombinedParams(searchParams),
      };
    } else if (hasGuideParams) {
      // Fallback to guide params if no main form params
      initialFormData = {
        ...defaultFormState,
        ...mapGuideParamsToFormState(searchParams),
      };
    } else {
      // No params, use defaults
      initialFormData = defaultFormState;
    }

    return { showDashboardInitially, initialFormData };
  }, [searchParams]); // Re-run only if search params change

  const { showDashboardInitially, initialFormData } = getInitialState();

  const [showDashboard, setShowDashboard] = useState(showDashboardInitially);
  const [currentFormData, setCurrentFormData] =
    useState<SolarRoiCalculatorParams>(initialFormData);
  const [shareStatus, setShareStatus] = useState<"idle" | "copied">("idle");

  // API Mutation
  const mutation = useMutation({
    mutationFn: calculateRoi,
    onSuccess: (data) => {
      console.log("Calculation successful:", data);
      setResults(data);
      if (!showDashboard) setShowDashboard(true); // Ensure dashboard visible
    },
    onError: (error) => {
      console.error("Calculation failed:", error);
      setResults(null); // Clear results on error
      if (!showDashboard) setShowDashboard(true); // Ensure dashboard visible
    },
  });

  // Debounced API call function
  const debouncedCalculate = useDebouncedCallback(
    (data: SolarRoiCalculatorParams) => {
      mutation.mutate(data);
    },
    750 // Debounce time in milliseconds (adjust as needed)
  );

  // Effect to run calculation when form data changes (debounced)
  useEffect(() => {
    // Only run calculation if dashboard is shown
    // And if the initial form data isn't the same as default (avoid calc on first load unless params are present)
    // Or if the current form data differs from the initial data derived from params
    if (showDashboard && currentFormData !== initialFormData) {
      console.log(
        "Form data changed, debouncing calculation...",
        currentFormData
      );
      debouncedCalculate(currentFormData);
      // Update URL silently as user changes inputs - ensure keys match schema
      // Explicitly create the object with types matching CombinedSearchParams
      const searchParamsToSet: Partial<CombinedSearchParams> = {
        // Use Partial<> as not all guide params are present
        batterySize: currentFormData.batterySize,
        solarSize: currentFormData.solarSize,
        usage: currentFormData.usage,
        // Convert CardinalDirection (hyphenated) to schema format (underscored)
        solarPanelDirection: currentFormData.solarPanelDirection.replace(
          "-",
          "_"
        ) as CombinedSearchParams["solarPanelDirection"],
        haveOrWillGetEv: currentFormData.haveOrWillGetEv,
        homeOccupancyDuringWorkHours:
          currentFormData.homeOccupancyDuringWorkHours,
        needFinance: currentFormData.needFinance,
      };
      // Filter out undefined values before navigating
      const filteredSearchParams = Object.entries(searchParamsToSet)
        .filter(([_, value]) => value !== undefined)
        .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});

      navigate({ search: filteredSearchParams as any, replace: true }); // Cast to any
    }
  }, [
    currentFormData,
    showDashboard,
    debouncedCalculate,
    initialFormData,
    navigate,
  ]); // Add initialFormData and navigate

  // Effect to run calculation ONCE when loaded with params
  useEffect(() => {
    const hasAnyParams = Object.keys(searchParams).length > 0;
    // If dashboard shown initially due to params, and no results yet, run calc
    if (
      showDashboardInitially &&
      hasAnyParams &&
      !results &&
      !mutation.isPending &&
      !mutation.isError
    ) {
      console.log("Initial load with params, calculating...", initialFormData);
      mutation.mutate(initialFormData);
    }
  }, [
    showDashboardInitially,
    initialFormData,
    results,
    mutation,
    searchParams,
  ]); // Dependencies

  // Simplified handler for form changes - just update state
  const handleFormDataChange = (updatedData: SolarRoiCalculatorParams) => {
    // Check if data actually changed to prevent infinite loops if child component triggers unnecessarily
    if (JSON.stringify(updatedData) !== JSON.stringify(currentFormData)) {
      setCurrentFormData(updatedData);
    }
  };

  // Handler for guide completion - navigate with guide params
  const handleGuideComplete = (guideAnswers: Record<string, string>) => {
    console.log("Guide complete, navigating with params:", guideAnswers);
    // Navigate will update searchParams, triggering initial state calculation and effects
    navigate({ to: "/", search: guideAnswers, replace: true });
    setShowDashboard(true); // Explicitly show dashboard after guide
  };

  // Share button handler
  const handleShare = async () => {
    const url = new URL(window.location.href);
    const params = new URLSearchParams();

    // Add current form data to params
    Object.entries(currentFormData).forEach(([key, value]) => {
      params.set(key, String(value));
    });

    url.search = params.toString();

    try {
      await navigator.clipboard.writeText(url.toString());
      setShareStatus("copied");
      console.log("Share URL copied:", url.toString());
      setTimeout(() => setShareStatus("idle"), 2000); // Reset after 2 seconds
    } catch (err) {
      console.error("Failed to copy share URL:", err);
      // TODO: Show error feedback to user
    }
  };

  const formatCurrency = (amount: number, _currency?: string) => {
    // Make currency optional
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
            {/* Main layout grid - swap the order of columns on desktop */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Results Column - Now on left for desktop */}
              <div className="lg:col-span-2 space-y-8 order-2 lg:order-1">
                {/* Loading/Initial State for Results Area */}
                {!results && mutation.isPending && (
                  <div className="text-center text-muted-foreground py-10">
                    Calculating... Please wait.
                  </div>
                )}
                {!results && !mutation.isPending && !mutation.isError && (
                  <div className="text-center text-muted-foreground py-10">
                    Adjust the inputs on the right to see your potential
                    savings.
                  </div>
                )}
                {mutation.isError && (
                  <div className="text-center text-red-600 py-10">
                    Calculation failed. Please check inputs or try again later.
                  </div>
                )}

                {/* Results Display (only if results exist) */}
                {results && (
                  <>
                    {/* ScoreCards */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                      <ScoreCard
                        title="Total System Cost"
                        value={formatCurrency(results.totalCost.amount)}
                      />
                      <ScoreCard
                        title="Yearly Savings"
                        value={formatCurrency(results.yearlySavings.amount)}
                      />
                      <ScoreCard
                        title="Payback Period"
                        value={`${results.paybackPeriod.years < 0 ? ">15" : results.paybackPeriod.years} Years`}
                      />
                      <ScoreCard
                        title={`ROI (${results.roiPercentage.periodYears} Years)`}
                        value={`${results.roiPercentage.percentage.toFixed(1)}%`}
                      />
                    </div>

                    {/* Chart */}
                    <div className="bg-card p-4 md:p-6 rounded-lg shadow-sm">
                      <h2 className="text-xl font-semibold mb-4 text-center">
                        Cumulative Savings Over Time
                      </h2>
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
                    </div>
                  </>
                )}
              </div>

              {/* Inputs Column - Now on right for desktop */}
              <div className="lg:col-span-1 space-y-6 order-1 lg:order-2">
                <RoiInputForm
                  formData={currentFormData}
                  onFormDataChange={handleFormDataChange}
                />
                {/* Share Button */}
                <Button
                  onClick={handleShare}
                  disabled={shareStatus === "copied"}
                  className="w-full"
                >
                  <Share2Icon className="mr-2 h-4 w-4" />
                  {shareStatus === "copied"
                    ? "Link Copied!"
                    : "Share Calculation"}
                </Button>
                <div className="hidden md:block">
                  <AffiliateBanner />
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Footer */}
      <div className="md:hidden">
        <AffiliateBanner />
      </div>

      {showDashboard && results && (
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
