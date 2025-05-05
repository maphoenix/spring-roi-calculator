import { useState, useEffect, useCallback } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { formatCurrency } from "@/lib/format-currency";
import { useRoi } from "@/hooks/roi";
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
import { Share2Icon, Loader2, ArrowLeft } from "lucide-react";
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

const mapGuideParamsToFormState = (
  params: CombinedSearchParams
): Partial<SolarRoiCalculatorParams> => {
  const mapping: Partial<SolarRoiCalculatorParams> = {};

  // Map houseSize to usage
  if (params.houseSize) {
    mapping.usage =
      params.houseSize === "small"
        ? 3000
        : params.houseSize === "medium"
          ? 4500
          : 6000;
  }

  // Map roofDirection to solarPanelDirection
  // The schema defines roofDirection with hyphens (e.g., "north-east")
  // CardinalDirection also uses hyphens.
  // So, use the value directly if it's valid and not "dont_know".
  if (params.roofDirection && params.roofDirection !== "dont_know") {
    // No replacement needed, use the hyphenated value directly from schema
    mapping.solarPanelDirection = params.roofDirection as CardinalDirection; // Use the hyphenated value
  } else if (params.roofDirection === "dont_know") {
    // Set a default if "Don't Know" is selected
    console.log(
      "Setting default solar panel direction to south from guide param 'dont_know'",
      params
    );
    mapping.solarPanelDirection = "south";
  }

  // Map hasEv to haveOrWillGetEv
  if (params.hasEv !== undefined) {
    // Check for undefined explicitly
    mapping.haveOrWillGetEv = params.hasEv === "yes";
  }

  // Map isHome to homeOccupancyDuringWorkHours
  if (params.isHome !== undefined) {
    // Check for undefined explicitly
    mapping.homeOccupancyDuringWorkHours = params.isHome === "yes";
  }

  // Map needsFinance (guide version) to needFinance (form version)
  if (params.needsFinance !== undefined) {
    // Check for undefined explicitly
    mapping.needFinance = params.needsFinance === "yes";
  }

  console.log("Mapped Guide Params:", mapping); // Add log

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

  // Convert the underscore version from URL params (e.g., "north_west")
  // back to the hyphenated version expected by CardinalDirection (e.g., "north-west")
  if (params.solarPanelDirection !== undefined) {
    formState.solarPanelDirection = params.solarPanelDirection.replace(
      "_",
      "-"
    ) as CardinalDirection;
  }

  if (params.haveOrWillGetEv !== undefined)
    formState.haveOrWillGetEv = params.haveOrWillGetEv;
  if (params.homeOccupancyDuringWorkHours !== undefined)
    formState.homeOccupancyDuringWorkHours =
      params.homeOccupancyDuringWorkHours;
  if (params.needFinance !== undefined)
    formState.needFinance = params.needFinance; // Main form version

  return formState;
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
  }, [searchParams]); // Dependency is correct

  const { showDashboardInitially, initialFormData } = getInitialState();

  // Initialize state - ensure currentFormData uses the calculated initialFormData
  const [showDashboard, setShowDashboard] = useState(false);
  const [currentFormData, setCurrentFormData] =
    useState<SolarRoiCalculatorParams>(initialFormData);
  const [shareStatus, setShareStatus] = useState<"idle" | "copied">("idle");
  const [isDebouncing, setIsDebouncing] = useState(false); // State for debounce indicator
  const mutation = useRoi(
    setIsDebouncing,
    setResults,
    showDashboard,
    setShowDashboard
  );

  // Debounced API call function - just calls mutate
  const debouncedCalculate = useDebouncedCallback(
    (data: SolarRoiCalculatorParams) => {
      console.log("Debounce timer fired. Mutating with:", data);
      mutation.mutate(data);
    },
    750 // Debounce delay
  );

  // Effect to run calculation when form data changes (debounced)
  useEffect(() => {
    // This effect runs when currentFormData changes, or other dependencies.
    // It schedules the debounced calculation and updates the URL.

    // Avoid running if the dashboard isn't shown OR
    // if a calculation mutation is already running.
    if (!showDashboard || mutation.isPending) {
      console.log(
        "Main effect skipped: dashboard not shown or mutation pending."
      );
      // Cancel any pending debounce from previous state changes if we bail early.
      debouncedCalculate.cancel();
      // If we were visually debouncing, but now skip, clear the indicator.
      // (Unless mutation is pending, in which case spinner might still be relevant)
      if (isDebouncing && !mutation.isPending) {
        setIsDebouncing(false);
      }
      return;
    }

    // Check if the current form data is actually different from the initial data
    // derived from the current URL params. This prevents running the calculation
    // immediately after the initial load effect runs or if the user reverts changes.
    // Compare stringified versions for a deep comparison of values.
    const isInitialState =
      JSON.stringify(currentFormData) === JSON.stringify(initialFormData);

    // Only proceed if it's NOT the initial state derived directly from initial load/params
    if (!isInitialState) {
      console.log(
        "Form data changed & not initial state, scheduling calculation and updating URL...",
        currentFormData
      );
      // Set debouncing state to true to show visual indicator
      setIsDebouncing(true);
      // Schedule the debounced calculation
      debouncedCalculate(currentFormData);

      // --- Update URL Silently ---
      const searchParamsToSet: Partial<CombinedSearchParams> = {
        batterySize: currentFormData.batterySize,
        solarSize: currentFormData.solarSize,
        usage: currentFormData.usage,
        solarPanelDirection: currentFormData.solarPanelDirection.replace(
          "-",
          "_"
        ) as CombinedSearchParams["solarPanelDirection"],
        haveOrWillGetEv: currentFormData.haveOrWillGetEv,
        homeOccupancyDuringWorkHours:
          currentFormData.homeOccupancyDuringWorkHours,
        needFinance: currentFormData.needFinance,
      };
      const filteredSearchParams = Object.entries(searchParamsToSet)
        .filter(([_, value]) => value !== undefined)
        .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});

      // Use URLSearchParams for robust comparison, ignoring key order
      const currentSearchString = new URLSearchParams(
        searchParams as any
      ).toString();
      const nextSearchString = new URLSearchParams(
        filteredSearchParams as any
      ).toString();

      // Only navigate if the params derived from state are different from current URL params
      if (currentSearchString !== nextSearchString) {
        console.log("Updating URL params because they differ.");
        navigate({ search: filteredSearchParams as any, replace: true });
      } else {
        console.log("URL params match, skipping navigate.");
        // If URL doesn't need updating, but we scheduled a calculation,
        // the isDebouncing flag remains true until the debounce fires/is cancelled.
      }
    } else {
      console.log(
        "Form data matches initial state, skipping debounced calculation and URL update."
      );
      // If the state reverted to initial, cancel any pending debounce and clear visual indicator.
      debouncedCalculate.cancel();
      setIsDebouncing(false);
    }

    // Cleanup function
    return () => {
      // console.log("Cleaning up main effect. Cancelling any pending debounce."); // Reduce console noise
      debouncedCalculate.cancel();
      // Do not reset isDebouncing here - let mutation callbacks handle it finally.
    };
  }, [
    currentFormData, // Primary trigger for changes
    initialFormData, // Needed for the isInitialState check
    showDashboard,
    debouncedCalculate,
    navigate,
    searchParams, // Need current params to decide if navigate is necessary
    mutation.isPending, // Avoid running when calculation is pending
    // Note: `isDebouncing` is NOT a dependency here
  ]);

  // Effect to run calculation ONCE when loaded with params (Initial Load)
  useEffect(() => {
    const hasAnyParams = Object.keys(searchParams).length > 0;
    // If dashboard shown initially due to params, and no results yet, and no mutation active
    // Check mutation status thoroughly to prevent re-running after first success/error
    if (
      showDashboardInitially &&
      hasAnyParams &&
      !results &&
      !mutation.isPending && // Ensure no mutation is currently running
      !mutation.isSuccess && // Ensure it hasn't already succeeded
      !mutation.isError // Ensure it hasn't already failed
    ) {
      console.log("Initial load with params, calculating...", initialFormData);
      // Don't set debouncing flag here, as this is an immediate calculation
      mutation.mutate(initialFormData);
    }
    // No cleanup needed for this effect specifically related to debounce
  }, [
    showDashboardInitially,
    initialFormData,
    results,
    searchParams,
    // Add mutation status flags as dependencies to prevent re-triggering if they change
    mutation.isPending,
    mutation.isSuccess,
    mutation.isError,
    // Explicitly depend on the mutate function if needed, though status flags are usually enough
    // mutation.mutate // Generally not needed if status flags are used
  ]); // Dependencies

  // Simplified handler for form changes - just update state
  const handleFormDataChange = (updatedData: SolarRoiCalculatorParams) => {
    // Basic check to prevent unnecessary updates if object reference is same
    if (updatedData === currentFormData) return;

    // Deeper check for value equality
    if (JSON.stringify(updatedData) !== JSON.stringify(currentFormData)) {
      console.log("handleFormDataChange: Setting new form data", updatedData);
      // Don't set debouncing here, let the useEffect handle it
      setCurrentFormData(updatedData);
    } else {
      // console.log("handleFormDataChange: Data is the same, not updating state."); // Reduce noise
    }
  };

  // Handler for guide completion - navigate with guide params
  const handleGuideComplete = (
    guideAnswers: Record<string, string | number>
  ) => {
    // Allow number for estimatedSolarSize etc.
    console.log("Guide complete, navigating with params:", guideAnswers);

    const paramsToNavigate = { ...guideAnswers };
    // Remove estimatedSolarSize if present, as it's not a direct URL param in the schema
    delete paramsToNavigate.estimatedSolarSize;

    // Navigate will update searchParams, triggering initial state calculation and effects
    // The search object keys should match the Zod schema keys
    navigate({ to: "/", search: paramsToNavigate, replace: true });
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
            {/* Go Back Button */}
            <Button
              variant="ghost"
              size="sm"
              className="mb-4 flex items-center space-x-2 text-muted-foreground hover:text-foreground"
            >
              <ArrowLeft className="h-4 w-4" />
              <span
                onClick={() => {
                  window.location.href = "/";
                }}
              >
                Start Form Again
              </span>
            </Button>

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
                {!results &&
                  !mutation.isPending &&
                  !isDebouncing &&
                  !mutation.isError && (
                    <div className="text-center text-muted-foreground py-10">
                      Adjust the inputs on the right to see your potential
                      savings.
                    </div>
                  )}
                {mutation.isError && !mutation.isPending && (
                  <div className="text-center text-red-600 py-10">
                    Calculation failed. Please check inputs or try again later.
                  </div>
                )}

                {/* Results Display (only if results exist) */}
                {results && (
                  <>
                    {/* ScoreCards */}
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
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
                    <div className="bg-card p-4 md:p-6 rounded-lg shadow-sm relative">
                      {/* Debounce/Loading Spinner Overlay with Animation */}
                      <AnimatePresence>
                        {/* Show spinner if debouncing OR if the mutation is pending */}
                        {(isDebouncing || mutation.isPending) && (
                          <motion.div
                            key="debounce-spinner"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            transition={{ duration: 0.2 }} // Quick fade
                            className="absolute inset-0 flex items-center justify-center bg-card/50 backdrop-blur-sm rounded-lg z-10"
                          >
                            <Loader2 className="h-8 w-8 animate-spin text-primary" />
                          </motion.div>
                        )}
                      </AnimatePresence>
                      <h2 className="text-xl font-semibold mb-4 text-center">
                        Cumulative Savings Over Time
                      </h2>
                      <div className="mt-2 md:mt-0 relative">
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
                              Payback period is longer than the system lifespan
                              ({results.roiPercentage.periodYears} years).
                            </p>
                          )}
                      </div>
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
      <div className="md:hidden mt-8">
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
