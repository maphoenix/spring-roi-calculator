"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import axiosClient from "@/lib/axios-client"; // Import the configured Axios client
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type {
  SolarRoiCalculatorParams,
  CardinalDirection,
} from "@/types/roi-calculator-types";
import type { RoiCalculationResponse } from "@/types/roi-api-response-types"; // Import the new response type

const directions: CardinalDirection[] = [
  "north",
  "north-east",
  "north-west",
  "south",
  "south-east",
  "south-west",
  "east",
  "west",
];

// Define the API call function using the configured client
const calculateRoi = async (
  formData: SolarRoiCalculatorParams
): Promise<RoiCalculationResponse> => {
  const { data } = await axiosClient.post<RoiCalculationResponse>(
    "/api/roi/calculate",
    formData
  );
  return data;
};

// Define props for the component, including the callback
interface RoiInputFormProps {
  onCalculationSuccess: (data: RoiCalculationResponse) => void;
  // Add onError callback? Maybe later.
}

export function RoiInputForm({ onCalculationSuccess }: RoiInputFormProps) {
  // State for form inputs
  const [formState, setFormState] = useState<SolarRoiCalculatorParams>({
    solarPanelDirection: "south",
    haveOrWillGetEv: false,
    homeOccupancyDuringWorkHours: true,
    needFinance: false,
    batterySize: 10, // Default value
    usage: 4500, // Default value
    solarSize: 5, // Default value
  });

  // Mutation hook for the API call
  const mutation = useMutation({
    mutationFn: calculateRoi,
    onSuccess: (data) => {
      console.log("Calculation successful in form:", data);
      // Call the callback prop to pass data up to the parent (index.tsx)
      onCalculationSuccess(data);
    },
    onError: (error) => {
      console.error("Calculation failed:", error);
      // TODO: Show error message to the user - maybe pass error up too?
    },
  });

  // Generic handler for input changes
  const handleChange = (field: keyof SolarRoiCalculatorParams, value: any) => {
    // Ensure numeric fields are stored as numbers
    const numericFields: (keyof SolarRoiCalculatorParams)[] = [
      "batterySize",
      "usage",
      "solarSize",
    ];
    const processedValue = numericFields.includes(field)
      ? Number(value) || 0
      : value;

    setFormState((prevState) => ({
      ...prevState,
      [field]: processedValue,
    }));
  };

  // Handle form submission
  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    console.log("Submitting form data:", formState);
    mutation.mutate(formState);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Calculator Inputs</CardTitle>
        <CardDescription>
          Adjust the parameters below to calculate your potential ROI.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="direction">Solar Panel Direction</Label>
            <Select
              value={formState.solarPanelDirection}
              onValueChange={(value) =>
                handleChange("solarPanelDirection", value as CardinalDirection)
              }
            >
              <SelectTrigger id="direction">
                <SelectValue placeholder="Select direction" />
              </SelectTrigger>
              <SelectContent>
                {directions.map((dir) => (
                  <SelectItem key={dir} value={dir}>
                    {dir.charAt(0).toUpperCase() +
                      dir.slice(1).replace("-", " ")}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex items-center justify-between space-x-2">
            <Label htmlFor="ev">Have/Will Get EV?</Label>
            <Switch
              id="ev"
              checked={formState.haveOrWillGetEv}
              onCheckedChange={(checked) =>
                handleChange("haveOrWillGetEv", checked)
              }
            />
          </div>

          <div className="flex items-center justify-between space-x-2">
            <Label htmlFor="occupancy">Home During Work Hours?</Label>
            <Switch
              id="occupancy"
              checked={formState.homeOccupancyDuringWorkHours}
              onCheckedChange={(checked) =>
                handleChange("homeOccupancyDuringWorkHours", checked)
              }
            />
          </div>

          <div className="flex items-center justify-between space-x-2">
            <Label htmlFor="finance">Need Finance?</Label>
            <Switch
              id="finance"
              checked={formState.needFinance}
              onCheckedChange={(checked) =>
                handleChange("needFinance", checked)
              }
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="battery">Battery Size (kWh)</Label>
            <Input
              id="battery"
              type="number"
              placeholder="e.g., 13.5"
              value={formState.batterySize}
              onChange={(e) => handleChange("batterySize", e.target.value)}
              step="0.1" // Allow decimals
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="usage">Annual Usage (kWh)</Label>
            <Input
              id="usage"
              type="number"
              placeholder="e.g., 4500"
              value={formState.usage}
              onChange={(e) => handleChange("usage", e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="solar">Solar System Size (kW)</Label>
            <Input
              id="solar"
              type="number"
              placeholder="e.g., 6.6"
              value={formState.solarSize}
              onChange={(e) => handleChange("solarSize", e.target.value)}
              step="0.1" // Allow decimals
            />
          </div>

          <Button
            type="submit"
            disabled={mutation.isPending}
            className="w-full"
          >
            {mutation.isPending ? "Calculating..." : "Calculate ROI"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
