"use client";

import { useState, useEffect } from "react";
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
import { Info } from "lucide-react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";

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

// Define props for the component
interface RoiInputFormProps {
  formData: SolarRoiCalculatorParams; // Receive current form data
  onFormDataChange: (newData: SolarRoiCalculatorParams) => void; // Callback to update parent state
  onCalculate: (formData: SolarRoiCalculatorParams) => void; // Callback to trigger calculation in parent
  isCalculating: boolean; // Receive loading state from parent
}

export function RoiInputForm({
  formData,
  onFormDataChange,
  onCalculate,
  isCalculating,
}: RoiInputFormProps) {
  // Local state now mirrors the formData prop
  const [formState, setFormState] =
    useState<SolarRoiCalculatorParams>(formData);

  // Effect to update local state if the formData prop changes from parent
  // (e.g., after guide completion or reset)
  useEffect(() => {
    setFormState(formData);
  }, [formData]);

  // Update handleChange to call onFormDataChange as well
  const handleChange = (
    field: keyof SolarRoiCalculatorParams,
    value: string | number | boolean // Adjusted type for direct value passing
  ) => {
    // Ensure numeric fields are stored as numbers
    const numericFields: (keyof SolarRoiCalculatorParams)[] = [
      "batterySize",
      "usage",
      "solarSize",
    ];
    let processedValue = value;
    if (numericFields.includes(field)) {
      // Handle potential empty string for number inputs, defaulting to 0
      processedValue = value === "" ? 0 : Number(value);
      if (isNaN(processedValue as number)) {
        processedValue = 0; // Fallback if conversion results in NaN
      }
    }

    const newState = {
      ...formState,
      [field]: processedValue,
    };
    setFormState(newState); // Update local state immediately for responsiveness
    onFormDataChange(newState); // Inform parent of the change
  };

  // Handle form submission - now calls the onCalculate prop
  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    console.log("Triggering recalculation with form data:", formState);
    onCalculate(formState);
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
        <TooltipProvider>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <div className="flex items-center space-x-1">
                <Label htmlFor="direction">Solar Panel Direction</Label>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-4 w-4 p-0"
                      type="button"
                    >
                      <Info className="h-4 w-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent className="max-w-xs">
                    <p className="text-center">
                      Facing south generally yields the most energy, but other
                      directions can be viable depending on roof shape and local
                      conditions.
                    </p>
                  </TooltipContent>
                </Tooltip>
              </div>
              <Select
                value={formState.solarPanelDirection}
                onValueChange={(value) =>
                  // Pass value directly
                  handleChange(
                    "solarPanelDirection",
                    value as CardinalDirection
                  )
                }
                disabled={isCalculating} // Disable while calculating
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
              <div className="flex items-center space-x-1">
                <Label htmlFor="ev">Have/Will Get EV?</Label>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-4 w-4 p-0"
                      type="button"
                    >
                      <Info className="h-4 w-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent className="max-w-xs">
                    <p className="text-center">
                      Having an Electric Vehicle increases household energy
                      usage, especially during off-peak charging times.
                    </p>
                  </TooltipContent>
                </Tooltip>
              </div>
              <Switch
                id="ev"
                checked={formState.haveOrWillGetEv}
                onCheckedChange={(checked) =>
                  // Pass value directly
                  handleChange("haveOrWillGetEv", checked)
                }
                disabled={isCalculating} // Disable while calculating
              />
            </div>

            <div className="flex items-center justify-between space-x-2">
              <div className="flex items-center space-x-1">
                <Label htmlFor="occupancy">Home During Work Hours?</Label>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-4 w-4 p-0"
                      type="button"
                    >
                      <Info className="h-4 w-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent className="max-w-xs">
                    <p className="text-center">
                      Being home during the day increases daytime energy usage,
                      allowing for more direct consumption of solar power.
                    </p>
                  </TooltipContent>
                </Tooltip>
              </div>
              <Switch
                id="occupancy"
                checked={formState.homeOccupancyDuringWorkHours}
                onCheckedChange={(checked) =>
                  // Pass value directly
                  handleChange("homeOccupancyDuringWorkHours", checked)
                }
                disabled={isCalculating} // Disable while calculating
              />
            </div>

            <div className="flex items-center justify-between space-x-2">
              <div className="flex items-center space-x-1">
                <Label htmlFor="finance">Need Finance?</Label>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-4 w-4 p-0"
                      type="button"
                    >
                      <Info className="h-4 w-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent className="max-w-xs">
                    <p className="text-center">
                      Select if you plan to finance the system purchase. This
                      affects the initial cost assumption (currently assumes
                      cash purchase).
                    </p>
                  </TooltipContent>
                </Tooltip>
              </div>
              <Switch
                id="finance"
                checked={formState.needFinance}
                onCheckedChange={(checked) =>
                  // Pass value directly
                  handleChange("needFinance", checked)
                }
                disabled={isCalculating} // Disable while calculating
              />
            </div>

            <div className="space-y-2">
              <div className="flex items-center space-x-1">
                <Label htmlFor="battery">Battery Size (kWh)</Label>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-4 w-4 p-0"
                      type="button"
                    >
                      <Info className="h-4 w-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent className="max-w-xs">
                    <p className="text-center">
                      The storage capacity of your battery system in
                      kilowatt-hours. Larger batteries store more excess solar
                      energy for later use.
                    </p>
                  </TooltipContent>
                </Tooltip>
              </div>
              <Input
                id="battery"
                type="number"
                placeholder="e.g., 13.5"
                value={formState.batterySize}
                // Use onInput for potentially better real-time updates with numbers?
                // Or keep onChange, ensuring handleChange handles number conversion robustly
                onChange={(e) => handleChange("batterySize", e.target.value)}
                step="0.1"
                disabled={isCalculating} // Disable while calculating
              />
            </div>

            <div className="space-y-2">
              <div className="flex items-center space-x-1">
                <Label htmlFor="usage">Annual Usage (kWh)</Label>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-4 w-4 p-0"
                      type="button"
                    >
                      <Info className="h-4 w-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent className="max-w-xs">
                    <p className="text-center">
                      Your estimated total household electricity consumption per
                      year in kilowatt-hours. Check your utility bills for
                      accuracy.
                    </p>
                  </TooltipContent>
                </Tooltip>
              </div>
              <Input
                id="usage"
                type="number"
                placeholder="e.g., 4500"
                value={formState.usage}
                onChange={(e) => handleChange("usage", e.target.value)}
                disabled={isCalculating} // Disable while calculating
              />
            </div>

            <div className="space-y-2">
              <div className="flex items-center space-x-1">
                <Label htmlFor="solar">Solar System Size (kW)</Label>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-4 w-4 p-0"
                      type="button"
                    >
                      <Info className="h-4 w-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent className="max-w-xs">
                    <p className="text-center">
                      The peak power output of your solar panel array in
                      kilowatts (kWp). This determines how much energy can be
                      generated.
                    </p>
                  </TooltipContent>
                </Tooltip>
              </div>
              <Input
                id="solar"
                type="number"
                placeholder="e.g., 6.6"
                value={formState.solarSize}
                onChange={(e) => handleChange("solarSize", e.target.value)}
                step="0.1"
                disabled={isCalculating} // Disable while calculating
              />
            </div>

            <Button
              type="submit"
              disabled={isCalculating} // Use isCalculating prop
              className="w-full"
            >
              {/* Update button text based on loading state */}
              {isCalculating ? "Calculating..." : "Update Calculation"}
            </Button>
          </form>
        </TooltipProvider>
      </CardContent>
    </Card>
  );
}
