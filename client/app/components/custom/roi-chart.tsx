"use client";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  ResponsiveContainer,
  ReferenceLine,
  Tooltip,
} from "recharts";

import {
  ChartContainer,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import { RoiChartData, RoiChartDataPoint } from "@/types/roi-response-types";

interface RoiChartProps {
  chartData: RoiChartData;
}

// Define colors for positive and negative lines
const positiveColor = "#22c55e"; // Green
const negativeColor = "#ef4444"; // Red

// Updated chart config for potentially two lines (though we style directly)
const chartConfig = {
  positiveSavings: {
    label: "Positive Savings",
    color: positiveColor,
  },
  negativeSavings: {
    label: "Negative Balance", // Or Initial Cost
    color: negativeColor,
  },
} satisfies ChartConfig;

export function RoiChart({ chartData }: RoiChartProps) {
  const formatCurrency = (value: number) => {
    // const currency = chartData.dataPoints[0]?.currency || ""; // No longer needed if always GBP
    // Always use £ symbol for GBP
    return `£${value.toLocaleString()}`;
  };

  // Prepare data for separate positive/negative lines
  const processedData = chartData.dataPoints.map((point) => ({
    ...point,
    positiveSavings:
      point.cumulativeSavings >= 0 ? point.cumulativeSavings : null,
    negativeSavings:
      point.cumulativeSavings < 0 ? point.cumulativeSavings : null,
  }));

  return (
    <ChartContainer
      config={chartConfig}
      className="min-h-[250px] w-full aspect-video"
    >
      <ResponsiveContainer width="100%" height="100%">
        <LineChart
          accessibilityLayer
          data={processedData} // Use processed data
          margin={{
            top: 10,
            right: 10,
            left: 10,
            bottom: 0,
          }}
        >
          <CartesianGrid vertical={false} strokeDasharray="3 3" />
          <XAxis
            dataKey="year"
            tickLine={false}
            axisLine={false}
            tickMargin={8}
            fontSize={12}
            name="Year"
          />
          <YAxis
            tickFormatter={formatCurrency}
            tickLine={false}
            axisLine={false}
            tickMargin={4}
            fontSize={12}
            width={80}
            name="Savings"
            domain={["auto", "auto"]}
          />
          <ReferenceLine
            y={0}
            stroke="hsl(var(--muted-foreground))"
            strokeDasharray="2 2"
            label={{
              value: "Break Even",
              position: "insideTopRight",
              dy: -10,
              fill: "hsl(var(--muted-foreground))",
              fontSize: 10,
            }}
          />
          <Tooltip
            cursor={{ strokeDasharray: "3 3" }}
            content={
              <ChartTooltipContent formatter={formatCurrency} indicator="dot" />
            }
          />
          <Line
            dataKey="negativeSavings"
            type="monotone"
            stroke={negativeColor}
            strokeWidth={2}
            dot={{ r: 4, fill: negativeColor }}
            activeDot={{ r: 6 }}
            connectNulls={false}
          />
          <Line
            dataKey="positiveSavings"
            type="monotone"
            stroke={positiveColor}
            strokeWidth={2}
            dot={{ r: 4, fill: positiveColor }}
            activeDot={{ r: 6 }}
            connectNulls={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </ChartContainer>
  );
}
