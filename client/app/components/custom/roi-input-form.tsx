"use client";

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

// TODO: Add state management (e.g., useState or react-hook-form) later

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

export function RoiInputForm() {
  // Placeholder values - replace with state
  const values: Partial<SolarRoiCalculatorParams> = {
    solarPanelDirection: "south",
    haveOrWillGetEv: false,
    homeOccupancyDuringWorkHours: true,
    needFinance: false,
    batterySize: 10,
    usage: 4500,
    solarSize: 5,
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Calculator Inputs</CardTitle>
        <CardDescription>
          Adjust the parameters below to calculate your potential ROI.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="direction">Solar Panel Direction</Label>
          <Select
            defaultValue={values.solarPanelDirection}
            // onValueChange={(value) => handleDirectionChange(value as CardinalDirection)}
          >
            <SelectTrigger id="direction">
              <SelectValue placeholder="Select direction" />
            </SelectTrigger>
            <SelectContent>
              {directions.map((dir) => (
                <SelectItem key={dir} value={dir}>
                  {dir.charAt(0).toUpperCase() + dir.slice(1).replace("-", " ")}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex items-center justify-between space-x-2">
          <Label htmlFor="ev">Have/Will Get EV?</Label>
          <Switch
            id="ev"
            checked={values.haveOrWillGetEv}
            // onCheckedChange={handleEvChange}
          />
        </div>

        <div className="flex items-center justify-between space-x-2">
          <Label htmlFor="occupancy">Home During Work Hours?</Label>
          <Switch
            id="occupancy"
            checked={values.homeOccupancyDuringWorkHours}
            // onCheckedChange={handleOccupancyChange}
          />
        </div>

        <div className="flex items-center justify-between space-x-2">
          <Label htmlFor="finance">Need Finance?</Label>
          <Switch
            id="finance"
            checked={values.needFinance}
            // onCheckedChange={handleFinanceChange}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="battery">Battery Size (kWh)</Label>
          <Input
            id="battery"
            type="number"
            placeholder="e.g., 13.5"
            defaultValue={values.batterySize}
            // onChange={handleBatteryChange}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="usage">Annual Usage (kWh)</Label>
          <Input
            id="usage"
            type="number"
            placeholder="e.g., 4500"
            defaultValue={values.usage}
            // onChange={handleUsageChange}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="solar">Solar System Size (kW)</Label>
          <Input
            id="solar"
            type="number"
            placeholder="e.g., 6.6"
            defaultValue={values.solarSize}
            // onChange={handleSolarSizeChange}
          />
        </div>

        {/* TODO: Add Submit Button */}
      </CardContent>
    </Card>
  );
}
