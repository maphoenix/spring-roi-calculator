// app/routes/index.tsx
import * as fs from "node:fs";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { createServerFn } from "@tanstack/react-start";
import { ScoreCard } from "@/components/custom/score-card";
import { RoiChart } from "@/components/custom/roi-chart";
import { RoiInputForm } from "@/components/custom/roi-input-form";
import {
  mockMonthlySavings,
  mockYearlySavings,
  mockPaybackPeriod,
  mockRoiChartData,
  mockRoiPercentage,
  mockTotalCost,
} from "@/lib/mock-data";

const filePath = "count.txt";

async function readCount() {
  return parseInt(
    await fs.promises.readFile(filePath, "utf-8").catch(() => "0")
  );
}

const getCount = createServerFn({
  method: "GET",
}).handler(() => {
  return readCount();
});

const updateCount = createServerFn({ method: "POST" })
  .validator((d: number) => d)
  .handler(async ({ data }) => {
    const count = await readCount();
    await fs.promises.writeFile(filePath, `${count + data}`);
  });

export const Route = createFileRoute("/")({
  component: IndexComponent,
});

function IndexComponent() {
  const formatCurrency = (amount: number, _currency: string) => {
    // Always use £ symbol for GBP
    return `£${amount.toLocaleString()}`;
  };

  return (
    <div className="container mx-auto p-4 md:p-8">
      <h1 className="text-4xl md:text-5xl font-extrabold mb-10 pb-2 text-center tracking-tight bg-gradient-to-r from-green-400 to-blue-500 text-transparent bg-clip-text">
        Green Energy Savings Calculator
      </h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Input Form */}
        <div className="lg:col-span-1">
          <RoiInputForm />
        </div>

        {/* Right Column: Dashboard Results */}
        <div className="lg:col-span-2 space-y-8">
          {/* Score Cards Row - Adjusted for 5 cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-4 xl:grid-cols-4 gap-4">
            {/* Added Total Cost Card (first) */}
            <ScoreCard
              title="Total System Cost"
              value={formatCurrency(
                mockTotalCost.amount,
                mockTotalCost.currency
              )}
            />
            {/* <ScoreCard
              title="Monthly Savings"
              value={formatCurrency(
                mockMonthlySavings.amount,
                mockMonthlySavings.currency
              )}
            /> */}
            <ScoreCard
              title="Yearly Savings"
              value={formatCurrency(
                mockYearlySavings.amount,
                mockYearlySavings.currency
              )}
            />
            <ScoreCard
              title="Payback Period"
              value={`${mockPaybackPeriod.years} Years`}
            />
            {/* New ROI Percentage Card */}
            <ScoreCard
              title={`ROI (${mockRoiPercentage.periodYears} Years)`}
              value={`${mockRoiPercentage.percentage}%`}
            />
          </div>

          {/* Chart Section */}
          <div className="bg-card p-4 md:p-6 rounded-lg shadow-sm">
            <h2 className="text-xl font-semibold mb-4 text-center">
              Cumulative Savings Over Time
            </h2>
            <RoiChart chartData={mockRoiChartData} />
            {mockRoiChartData.breakEvenYear && (
              <p className="text-sm text-muted-foreground mt-4 text-center">
                Estimated break-even point around year{" "}
                {mockRoiChartData.breakEvenYear}.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
