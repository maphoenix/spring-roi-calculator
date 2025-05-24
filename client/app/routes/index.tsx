import { useState, useEffect, useCallback, useMemo } from "react";
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
  needsFinance: z.enum(["yes", "no"]).optional(),
  // Main form params (use zod transforms for types)
  batterySize: z.coerce.number().min(0).max(20).optional(),
  solarSize: z.coerce.number().min(1.5).max(20).optional(),
  usage: z.coerce.number().min(1500).max(20000).optional(),
  solarPanelDirection: z
    .enum([
      "north",
      "north_east", // Use underscore for URL param key consistency
      "north_west",
      "south",
      "south_east",
      "south_west",
      "east",
      "west",
    ])
    .optional(),
  haveOrWillGetEv: z.coerce.boolean().optional(),
  homeOccupancyDuringWorkHours: z.coerce.number().min(1).max(5).optional(),
  needFinance: z.coerce.boolean().optional(), // Main form param version
});

// Infer the type from the schema
type CombinedSearchParams = z.infer<typeof combinedSearchParamsSchema>;

// Define default form state values (used when no relevant params are present)
const defaultFormState: SolarRoiCalculatorParams = {
  solarPanelDirection: "south", // Use hyphenated for internal state
  haveOrWillGetEv: false,
  homeOccupancyDuringWorkHours: 3, // Level 3 = Sometimes home (middle value of 1-5 scale)
  needFinance: false,
  batterySize: 10, // Within new range 0-20 kWh
  usage: 4500, // Within new range 1500-20000 kWh
  solarSize: 5, // Within new range 1.5-20 kW
};

export const Route = createFileRoute("/")({
  validateSearch: zodValidator(combinedSearchParamsSchema),
  component: IndexComponent,
});

// Function to map guide params to form state (ensure values match form state types)
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

    // Map houseSize to solarSize
    mapping.solarSize =
      params.houseSize === "small"
        ? 1.5
        : params.houseSize === "medium"
          ? 3.2
          : 4.8;
  }

  // Map roofDirection to solarPanelDirection (hyphenated format)
  if (params.roofDirection && params.roofDirection !== "dont_know") {
    mapping.solarPanelDirection = params.roofDirection as CardinalDirection;
  } else if (params.roofDirection === "dont_know") {
    mapping.solarPanelDirection = "south"; // Default if 'dont_know'
  }

  // Map hasEv to haveOrWillGetEv (boolean)
  if (params.hasEv !== undefined) {
    mapping.haveOrWillGetEv = params.hasEv === "yes";
  }

  // Map homeOccupancyDuringWorkHours directly (guide now uses numeric 1-5)
  if (params.homeOccupancyDuringWorkHours !== undefined) {
    mapping.homeOccupancyDuringWorkHours = params.homeOccupancyDuringWorkHours;
  }

  // Map needsFinance (guide) to needFinance (form) (boolean)
  if (params.needsFinance !== undefined) {
    mapping.needFinance = params.needsFinance === "yes";
  }

  // console.log("Mapped Guide Params to Form State:", mapping); // Reduce noise
  return mapping;
};

// Function to get form state directly from combined search params (ensure values match form state types)
const getFormStateFromCombinedParams = (
  params: CombinedSearchParams
): Partial<SolarRoiCalculatorParams> => {
  const formState: Partial<SolarRoiCalculatorParams> = {};

  if (params.batterySize !== undefined)
    formState.batterySize = params.batterySize;
  if (params.solarSize !== undefined) formState.solarSize = params.solarSize;
  if (params.usage !== undefined) formState.usage = params.usage;

  // Convert underscore version from URL to hyphenated for internal state
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
    formState.needFinance = params.needFinance;

  // console.log("Parsed Form State from Combined Params:", formState); // Reduce noise
  return formState;
};

// Helper function to derive the complete state from search params
const deriveStateFromParams = (
  searchParams: CombinedSearchParams
): SolarRoiCalculatorParams => {
  // Check for the presence of *any* parameter defined in the main form section of the schema
  const hasMainFormParams = Object.keys(searchParams).some(
    (key) =>
      [
        "batterySize",
        "solarSize",
        "usage",
        "solarPanelDirection",
        "haveOrWillGetEv",
        "homeOccupancyDuringWorkHours",
        "needFinance",
      ].includes(key) &&
      searchParams[key as keyof CombinedSearchParams] !== undefined
  );

  // Check for the presence of *any* parameter defined in the guide section of the schema
  const hasGuideParams = Object.keys(searchParams).some(
    (key) =>
      ["houseSize", "roofDirection", "hasEv", "needsFinance"].includes(key) &&
      searchParams[key as keyof CombinedSearchParams] !== undefined
  );

  let derivedState: SolarRoiCalculatorParams;

  if (hasMainFormParams) {
    // Prioritize main form params - merge them onto defaults
    // console.log("Deriving state: Prioritizing main form params"); // Noise reduction
    derivedState = {
      ...defaultFormState,
      ...getFormStateFromCombinedParams(searchParams),
    };
  } else if (hasGuideParams) {
    // Fallback to guide params - merge them onto defaults
    // console.log("Deriving state: Falling back to guide params"); // Noise reduction
    derivedState = {
      ...defaultFormState,
      ...mapGuideParamsToFormState(searchParams),
    };
  } else {
    // No relevant params, use defaults
    // console.log("Deriving state: Using default form state"); // Noise reduction
    derivedState = { ...defaultFormState }; // Use spread to avoid mutation
  }
  // console.log("Derived State from Params:", derivedState); // Reduce noise
  return derivedState;
};

// Helper function to convert form state to URL search params suitable for navigation
const formStateToSearchParams = (
  state: SolarRoiCalculatorParams
): Partial<CombinedSearchParams> => {
  const params: Partial<CombinedSearchParams> = {
    batterySize: state.batterySize,
    solarSize: state.solarSize,
    usage: state.usage,
    // Convert hyphenated internal state back to underscore for URL
    solarPanelDirection: state.solarPanelDirection.replace(
      "-",
      "_"
    ) as CombinedSearchParams["solarPanelDirection"],
    haveOrWillGetEv: state.haveOrWillGetEv,
    homeOccupancyDuringWorkHours: state.homeOccupancyDuringWorkHours,
    needFinance: state.needFinance,
  };
  // Filter out undefined values before returning
  return Object.entries(params)
    .filter(([_, value]) => value !== undefined)
    .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});
};

// Affiliate links for the top promotional message (same as AffiliateBanner)
const affiliateLinks = [
  {
    name: "James' Link",
    url: "https://share.octopus.energy/sky-hare-157",
  },
  {
    name: "Dad's Link",
    url: "https://share.octopus.energy/happy-run-144",
  },
];

function IndexComponent() {
  const [results, setResults] = useState<RoiCalculationResponse | null>(null);
  const searchParams = Route.useSearch(); // Use the validated/typed search params from the router
  const navigate = useNavigate();

  // Random affiliate link selection for top promotional message
  const [selectedPromoLink, setSelectedPromoLink] = useState(affiliateLinks[0]);

  // Calculate initial state ONCE based on the search params present on mount
  // UseMemo ensures this doesn't re-run on every render, only if searchParams ref changes (which it shouldn't needlessly)
  const initialDerivedState = useMemo(() => {
    // console.log("Calculating initialDerivedState with searchParams:", searchParams); // Debug log
    return deriveStateFromParams(searchParams);
  }, [searchParams]); // Depend on searchParams from the router

  // Determine if dashboard should be shown initially based on *any* relevant param in the initial searchParams
  const showDashboardInitially = useMemo(() => {
    const hasAnyRelevantParam = Object.keys(searchParams).some(
      (key) =>
        combinedSearchParamsSchema.shape.hasOwnProperty(key) &&
        searchParams[key as keyof CombinedSearchParams] !== undefined
    );
    // console.log("Calculating showDashboardInitially:", hasAnyRelevantParam, "from searchParams:", searchParams); // Debug log
    return hasAnyRelevantParam;
  }, [searchParams]); // Depend on searchParams from the router

  // State Management
  const [showDashboard, setShowDashboard] = useState(showDashboardInitially);
  const [currentFormData, setCurrentFormData] =
    useState<SolarRoiCalculatorParams>(initialDerivedState); // Initialize with derived state
  const [shareStatus, setShareStatus] = useState<"idle" | "copied">("idle");
  // isDebouncing indicates that the user has changed input and we are waiting for the debounce timer to update the URL
  const [isDebouncing, setIsDebouncing] = useState(false);
  const mutation = useRoi(
    setIsDebouncing, // Pass this down - useRoi's onMutate/onSettled will set it to false when API call starts/finishes
    setResults,
    showDashboard,
    setShowDashboard
  );

  // Random selection for promotional link on component mount
  useEffect(() => {
    const randomIndex = Math.random() < 0.5 ? 0 : 1;
    setSelectedPromoLink(affiliateLinks[randomIndex]);
  }, []);

  // --- Debounced Navigation ---
  // This function updates the URL after a delay when form inputs change
  const debouncedNavigate = useDebouncedCallback(
    (data: SolarRoiCalculatorParams) => {
      // console.log("Debounce timer fired for navigation. Updating URL..."); // Reduce noise
      setIsDebouncing(false); // Turn off debounce indicator *when the navigation occurs*

      const nextSearchParams = formStateToSearchParams(data);

      // Get the *current* search params from the router hook scope *at the time of firing*
      // Use the 'searchParams' variable captured by this callback's closure
      const currentRouterParamsObject = searchParams; // Use the hook state directly

      // Compare the *main form params* derived from the current URL with the ones we are about to set
      // This prevents unnecessary navigation if the URL was updated externally to match the target state already
      const currentFormParamsFromUrl = formStateToSearchParams(
        deriveStateFromParams(currentRouterParamsObject)
      );

      if (
        JSON.stringify(nextSearchParams) !==
        JSON.stringify(currentFormParamsFromUrl)
      ) {
        // console.log("Navigating with new search params:", nextSearchParams); // Reduce noise
        // Use functional update with navigate to merge/replace params correctly
        // We replace entirely with the main form params, effectively clearing any guide-specific params
        navigate({
          // Provide the plain object. Cast to 'any' if linter still complains despite correct structure.
          search: nextSearchParams as any,
          replace: true, // Use replace to avoid polluting browser history
        });
      } else {
        // console.log("URL params already match target state, skipping navigation."); // Reduce noise
      }
      // The URL change triggered by navigate() will cause the main useEffect to run and handle the calculation
    },
    750 // Debounce delay
  );

  // --- Effect to React to URL Changes ---
  // This is the CORE effect. It runs whenever searchParams (from router) change.
  useEffect(() => {
    // console.log("URL searchParams changed (useEffect):", searchParams); // Reduce noise

    // 1. Derive the canonical state based *only* on the current searchParams
    const derivedState = deriveStateFromParams(searchParams);

    // 2. Sync local form state (currentFormData) ONLY IF it differs from the URL-derived state.
    //    This ensures the form visually updates if the URL changes externally (e.g., back button).
    if (JSON.stringify(currentFormData) !== JSON.stringify(derivedState)) {
      // console.log("Syncing local form state (currentFormData) to match URL-derived state."); // Reduce noise
      setCurrentFormData(derivedState);
      // If the URL change *wasn't* triggered by our debouncedNavigate, cancel any pending debounce
      // (This handles cases like back/forward button causing URL change during a debounce)
      if (isDebouncing) {
        // console.log("URL changed externally during debounce, cancelling pending navigation."); // Reduce noise
        debouncedNavigate.cancel();
        setIsDebouncing(false); // Clear debounce state as the URL is now the source of truth
      }
    }

    // 3. Determine if the dashboard should be shown based on the *current* params
    const shouldShowDashboard = Object.keys(searchParams).some(
      (key) =>
        combinedSearchParamsSchema.shape.hasOwnProperty(key) &&
        searchParams[key as keyof CombinedSearchParams] !== undefined
    );
    if (showDashboard !== shouldShowDashboard) {
      // console.log(`Updating showDashboard state from ${showDashboard} to ${shouldShowDashboard}`); // Reduce noise
      setShowDashboard(shouldShowDashboard);
    }

    // 4. Trigger calculation IF the dashboard should be visible AND the derived state is valid
    if (shouldShowDashboard) {
      // console.log("Dashboard is visible, considering calculation trigger..."); // Reduce noise
      // Optional: Add validation check here if deriveStateFromParams could produce invalid states
      // e.g., if (!isValidState(derivedState)) { console.error("Invalid state derived"); return; }

      // Trigger mutation only if state is different from current mutation variables or not pending/successful
      const isDifferentData =
        !mutation.variables ||
        JSON.stringify(mutation.variables) !== JSON.stringify(derivedState);
      const shouldMutate =
        (!mutation.isPending && !mutation.isSuccess) ||
        (mutation.isSuccess && isDifferentData) ||
        (mutation.isError && isDifferentData);

      if (shouldMutate) {
        // console.log("Triggering calculation with derived state:", derivedState); // Reduce noise
        setResults(null); // Clear previous results immediately before new calculation
        mutation.mutate(derivedState);
      } else if (mutation.isPending && !isDifferentData) {
        // console.log("Skipping mutation: Already pending for the same data."); // Reduce noise
      } else if (mutation.isSuccess && !isDifferentData) {
        // console.log("Skipping mutation: Already succeeded with the same data."); // Reduce noise
        // Ensure results are consistent if somehow cleared
        if (!results) setResults(mutation.data);
      }
    } else {
      // console.log("Dashboard not visible, skipping calculation."); // Reduce noise
      // If dashboard is hidden, clear results
      if (results) setResults(null);
      // Cancel mutation if it was pending for a state that's no longer relevant?
      // Cautious about this - might cancel valid background calcs if logic is complex.
      // if (mutation.isPending) mutation.reset(); // Or specific cancel API if available
    }

    // No cleanup needed here specifically for debounce, handled elsewhere.
  }, [searchParams, navigate]); // Primary dependency is searchParams. Add navigate for safety. Avoid adding state setters or mutation directly.

  // --- Form Input Handler ---
  // Updates local state immediately for UI, then triggers debounced URL update
  const handleFormDataChange = useCallback(
    (updatedData: SolarRoiCalculatorParams) => {
      // Basic check to prevent unnecessary updates if object reference is same
      if (updatedData === currentFormData) return;

      // Deeper check for value equality
      if (JSON.stringify(updatedData) !== JSON.stringify(currentFormData)) {
        // console.log("handleFormDataChange: Updating local state and scheduling URL update", updatedData); // Reduce noise
        // Update local state immediately for responsive UI
        setCurrentFormData(updatedData);
        // Set debouncing flag visually ONLY if not already debouncing
        // (prevents flicker if user types fast within debounce interval)
        if (!isDebouncing) {
          setIsDebouncing(true);
        }
        // Schedule the URL update (will reset isDebouncing when it fires)
        debouncedNavigate(updatedData);
      }
    },
    [currentFormData, debouncedNavigate, isDebouncing, setIsDebouncing]
  ); // Add deps

  // Handler for guide completion - navigate with guide params
  const handleGuideComplete = useCallback(
    (guideAnswers: Record<string, string | number>) => {
      // console.log("Guide complete, navigating with params:", guideAnswers); // Reduce noise

      const paramsToNavigate: Partial<CombinedSearchParams> = {};
      // Map guideAnswers keys to CombinedSearchParams keys directly
      for (const key in guideAnswers) {
        // Ensure the key from guideAnswers is actually part of our schema
        if (combinedSearchParamsSchema.shape.hasOwnProperty(key)) {
          // We explicitly exclude estimatedSolarSize as it's not a direct URL param
          if (key !== "estimatedSolarSize") {
            // Assign the value, relying on schema validation during navigation
            paramsToNavigate[key as keyof CombinedSearchParams] = guideAnswers[
              key
            ] as any;
          }
        }
      }

      // console.log("Navigating from guide with params:", paramsToNavigate); // Reduce noise
      // Navigate will update searchParams, triggering the main useEffect
      // Replace ensures we don't add the guide step itself to history
      navigate({ to: "/", search: paramsToNavigate, replace: true });
      // No need to manually setShowDashboard(true) here; the main useEffect will handle it based on the new params.
    },
    [navigate]
  ); // Add deps

  // Share button handler
  const handleShare = async () => {
    const url = new URL(window.location.href);
    // Generate params based on the *current* form state displayed to the user
    const params = new URLSearchParams(
      formStateToSearchParams(currentFormData) as Record<string, string>
    );
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

  // Function to go back to the guide view (clear params)
  const handleGoBack = () => {
    // console.log("handleGoBack: Navigating to clear params."); // Reduce noise
    // Clear all search params and navigate back to the root
    navigate({ to: "/", search: {}, replace: true });
    // The main useEffect will detect the empty params and set showDashboard(false) and clear results.
  };

  return (
    <div className="bg-gradient-to-br from-background via-purple-50/10 to-violet-50/15">
      <div className="container mx-auto p-4 md:p-8">
        {/* Top Left Promotional Message - Only show on chart page */}
        {showDashboard && (
          <div className="flex justify-start mb-4">
            <a
              href={selectedPromoLink.url}
              target="_blank"
              rel="noopener noreferrer"
              className="bg-gradient-to-r from-purple-50 to-purple-100 border border-purple-200 rounded-lg px-3 py-2 shadow-sm hover:shadow-md transition-shadow duration-200 cursor-pointer hover:from-purple-100 hover:to-purple-150"
            >
              <p className="text-xs font-medium text-purple-800">
                💡{" "}
                <span className="font-semibold">£50 for you, £50 for us!</span>{" "}
                Switch to Octopus Energy now
              </p>
            </a>
          </div>
        )}

        {!showDashboard ? (
          <motion.div
            key="guide"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.5 }}
            className="min-h-[80vh] flex items-center justify-center"
          >
            <div className="w-full max-w-3xl mx-auto px-4">
              {/* Pass initial data if available from params (though usually guide starts clean) */}
              <SimplifiedGuide onComplete={handleGuideComplete} />
            </div>
          </motion.div>
        ) : (
          <>
            {/* Go Back Button */}
            <Button
              variant="ghost"
              size="sm"
              onClick={handleGoBack} // Use the new handler
              className="mb-4 -ml-2 flex items-center space-x-2 text-muted-foreground hover:text-foreground" // Adjust margin for alignment
            >
              <ArrowLeft className="h-4 w-4" />
              <span>Start Over</span>
            </Button>

            {/* Main layout grid - swap the order of columns on desktop */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Results Column - Now on left for desktop */}
              <div className="lg:col-span-2 space-y-8 order-2 lg:order-1">
                {/* Loading/Initial State for Results Area */}
                {/* Show specific loading message only when mutation is actually pending */}
                {mutation.isPending && (
                  <div className="text-center text-muted-foreground py-10 min-h-[100px] flex items-center justify-center">
                    <Loader2 className="h-6 w-6 animate-spin inline mr-2" />
                    Calculating...
                  </div>
                )}
                {/* Show initial prompt if not loading and no results/error */}
                {!mutation.isPending && !results && !mutation.isError && (
                  <div className="text-center text-muted-foreground py-10 min-h-[100px] flex items-center justify-center">
                    Adjust the inputs on the right to calculate your potential
                    savings.
                  </div>
                )}
                {/* Error state */}
                {!mutation.isPending && mutation.isError && (
                  <div className="text-center text-red-600 py-10 min-h-[100px] flex flex-col items-center justify-center">
                    <span>Calculation failed.</span>
                    <span className="text-sm text-red-500 mt-1">
                      {mutation.error?.message ||
                        "Please check inputs or try again."}
                    </span>
                  </div>
                )}

                {/* Results Display (only if results exist and not pending) */}
                {!mutation.isPending && results && (
                  <>
                    {/* ScoreCards */}
                    <>
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
                    </>

                    {/* Chart */}
                    <>
                      <div className="bg-card p-4 md:p-6 rounded-lg shadow-sm relative">
                        {/* Debounce Indicator Overlay (Subtle) */}
                        {/* {isDebouncing && (
                          <>
                            <Loader2 className="h-4 w-4 animate-spin text-primary/80" />
                          </>
                        )} */}
                        <div className="text-center mb-3">
                          <p className="text-sm text-muted-foreground">
                            We're working to get you the best deal with Octopus
                          </p>
                        </div>
                        <h2 className="text-xl font-semibold mb-2 text-center">
                          Cumulative Savings Over Time
                        </h2>
                        <div className="text-center mb-4">
                          <p className="text-sm text-green-600 font-medium flex items-center justify-center space-x-1">
                            <span>🌳</span>
                            <span>
                              Every person that switches to solar saves 100
                              trees per year
                            </span>
                          </p>
                        </div>
                        <div className="mt-2 md:mt-0 relative">
                          {/* Ensure chartData is passed correctly */}
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
                                Payback period may be longer than the system
                                lifespan ({results.roiPercentage.periodYears}{" "}
                                years).
                              </p>
                            )}
                        </div>

                        {/* Inspirational Environmental Quote Section */}
                        <div className="mt-8 p-6 bg-gradient-to-r from-green-50 via-emerald-50 to-teal-50 border border-green-200 rounded-lg shadow-sm">
                          <div className="text-center">
                            <div className="text-2xl mb-2">🌱</div>
                            <blockquote className="text-lg font-medium text-green-800 italic mb-2">
                              {(() => {
                                const quotes = [
                                  "Every person that switches to solar saves 100 trees per year",
                                  "Solar power: turning sunshine into savings and sustainability",
                                  "A solar panel today keeps carbon emissions away",
                                  "Choose solar, choose a cleaner future for your children",
                                  "Every kilowatt of solar power is a victory for the planet",
                                  "Solar energy: the sun never sends you a bill",
                                  "Going solar isn't just smart economics, it's environmental leadership",
                                  "Your roof can be your power plant and the planet's ally",
                                  "Solar power: where financial savings meet environmental impact",
                                  "Every solar installation is a vote for a sustainable future",
                                  "Be the hero your electric bill never thought you could be",
                                  "Solar panels: because the sun works for free and never asks for a raise",
                                  "Stop renting energy, start owning sunshine",
                                  "Your roof is sitting on a goldmine of free electricity",
                                  "Solar power: making your neighbors jealous since day one",
                                  "Why pay for electricity when the sun is literally throwing energy at your house?",
                                  "Solar panels turn your roof into a money-printing machine",
                                  "Every solar panel installed is another nail in fossil fuel's coffin",
                                  "Be so solar-powered that your electric company misses you",
                                  "Solar energy: the ultimate flex on your utility company",
                                  "Your future self will thank you for going solar today",
                                  "Solar power: because paying for electricity is so last century",
                                  "Join the solar revolution - your wallet and the planet will love you",
                                  "Solar panels: the gift that keeps on giving... for 25+ years",
                                  "Stop feeding the grid, start owning your power",
                                  "Solar energy: turning your biggest expense into your best investment",
                                  "Be the change you want to see on your electricity bill",
                                  "Solar power: because free energy is the best energy",
                                  "Your roof called - it wants to make you money",
                                  "Solar panels: the smartest thing you can put on your roof besides a brain",
                                  "Going solar: the only time buying the sun actually makes sense",
                                  "Solar energy: making environmentalists and accountants happy since forever",
                                  "Why rent from the sun when you can own a piece of it?",
                                  "Solar power: the ultimate renewable relationship",
                                  "Your carbon footprint just called - it wants to go on a diet",
                                ];
                                return quotes[
                                  Math.floor(Math.random() * quotes.length)
                                ];
                              })()}
                            </blockquote>
                            <p className="text-sm text-green-600 font-semibold">
                              🌍 Make a difference with every ray of sunshine
                            </p>
                          </div>
                        </div>
                      </div>
                    </>
                  </>
                )}
              </div>

              {/* Inputs Column - Now on right for desktop */}
              <div className="lg:col-span-1 space-y-6 order-1 lg:order-2">
                {/* Pass the local state which is kept in sync with URL */}
                <RoiInputForm
                  formData={currentFormData}
                  onFormDataChange={handleFormDataChange}
                />
                {/* Share Button */}
                <Button
                  onClick={handleShare}
                  disabled={shareStatus === "copied" || !results} // Disable if no results to share
                  className="w-full"
                  variant="outline" // Less prominent than main action
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
          </>
        )}

        {/* Footer */}
        <div className="md:hidden mt-8">
          <AffiliateBanner />
        </div>

        {/* Show footer only when dashboard is visible and has results */}
        {showDashboard && results && !mutation.isPending && (
          <footer className="mt-12 pt-6 border-t border-border/40">
            <p className="text-center text-sm text-muted-foreground">
              Disclaimer: This calculator provides approximate estimations based
              on standard assumptions and the inputs provided. Results should be
              used as a guide only and supplemented with professional
              assessments.
            </p>
          </footer>
        )}
      </div>
    </div>
  );
}
