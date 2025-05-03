// app/routes/index.tsx
import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { ScoreCard } from "@/components/custom/score-card";
import { RoiChart } from "@/components/custom/roi-chart";
import { RoiInputForm } from "@/components/custom/roi-input-form";
import type { RoiCalculationResponse } from "@/types/roi-api-response-types";

export const Route = createFileRoute("/")({
  component: IndexComponent,
});

function IndexComponent() {
  const [results, setResults] = useState<RoiCalculationResponse | null>(null);

  const handleCalculationSuccess = (data: RoiCalculationResponse) => {
    console.log("Received calculation results in index:", data);
    setResults(data);
  };

  const formatCurrency = (amount: number, _currency: string) => {
    return `£${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  return (
    <div className="container mx-auto p-4 md:p-8">
      <h1 className="text-4xl md:text-5xl font-extrabold mb-10 pb-2 text-center tracking-tight bg-gradient-to-r from-green-400 to-blue-500 text-transparent bg-clip-text">
        Green Energy Savings Calculator
      </h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-1">
          <RoiInputForm onCalculationSuccess={handleCalculationSuccess} />
        </div>

        <div className="lg:col-span-2 space-y-8">
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

          <div className="bg-card p-4 md:p-6 rounded-lg shadow-sm">
            <h2 className="text-xl font-semibold mb-4 text-center">
              Cumulative Savings Over Time
            </h2>
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
              <div className="text-center text-muted-foreground py-10">
                Enter your details and click "Calculate ROI" to see the results.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
