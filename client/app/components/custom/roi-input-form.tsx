"use client";

import { useState, useEffect, useMemo } from "react";
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
import { Slider } from "@/components/ui/slider";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import type {
  SolarRoiCalculatorParams,
  CardinalDirection,
} from "@/types/roi-calculator-types";
import { Info, Sun, BatteryCharging, Car } from "lucide-react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { motion, AnimatePresence } from "framer-motion";

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

const AVERAGE_PANEL_KW = 0.4;
const MIN_SOLAR_SIZE = 2;
const MAX_SOLAR_SIZE = 20;
const MIN_USAGE = 1000;
const MAX_USAGE = 20000;
const MAX_BATTERY_SIZE = 50;

interface RoiInputFormProps {
  formData: SolarRoiCalculatorParams;
  onFormDataChange: (newData: SolarRoiCalculatorParams) => void;
}

export function RoiInputForm({
  formData,
  onFormDataChange,
}: RoiInputFormProps) {
  const [formState, setFormState] =
    useState<SolarRoiCalculatorParams>(formData);

  const [isSolarEnabled, setIsSolarEnabled] = useState<boolean>(
    formData.solarSize > 0
  );
  const [isBatteryEnabled, setIsBatteryEnabled] = useState<boolean>(
    formData.batterySize > 0
  );
  const [prevSolarSize, setPrevSolarSize] = useState<number>(
    formData.solarSize > 0 ? formData.solarSize : MIN_SOLAR_SIZE
  );
  const [prevBatterySize, setPrevBatterySize] = useState<number>(
    formData.batterySize > 0 ? formData.batterySize : 2.5
  );

  const [batteryIncrement, setBatteryIncrement] = useState<number>(2.5);

  useEffect(() => {
    setFormState(formData);
    const solarEnabled = formData.solarSize > 0;
    const batteryEnabled = formData.batterySize > 0;
    setIsSolarEnabled(solarEnabled);
    setIsBatteryEnabled(batteryEnabled);
    if (solarEnabled) setPrevSolarSize(formData.solarSize);
    if (batteryEnabled) setPrevBatterySize(formData.batterySize);
  }, [formData]);

  const minBatterySize = useMemo(
    () => (isBatteryEnabled ? batteryIncrement : 0),
    [isBatteryEnabled, batteryIncrement]
  );

  const solarSliderStep = 0.1;
  const batterySliderStep = batteryIncrement;

  const handleSliderChange = (
    field: keyof SolarRoiCalculatorParams,
    value: number[]
  ) => {
    const numericValue = value[0];
    const newState = {
      ...formState,
      [field]: numericValue,
    };
    setFormState(newState);
    onFormDataChange(newState);

    if (field === "solarSize" && isSolarEnabled) {
      setPrevSolarSize(numericValue);
    }
    if (field === "batterySize" && isBatteryEnabled) {
      setPrevBatterySize(numericValue);
    }
  };

  const handleSelectChange = (
    field: keyof SolarRoiCalculatorParams,
    value: string
  ) => {
    const newState = {
      ...formState,
      [field]: value,
    };
    setFormState(newState);
    onFormDataChange(newState);
  };

  const handleSwitchChange = (
    field: keyof SolarRoiCalculatorParams,
    checked: boolean
  ) => {
    const newState = {
      ...formState,
      [field]: checked,
    };
    setFormState(newState);
    onFormDataChange(newState);
  };

  const handleToggleSolar = (enabled: boolean) => {
    setIsSolarEnabled(enabled);
    const newSolarSize = enabled ? prevSolarSize : 0;
    const newState = {
      ...formState,
      solarSize: newSolarSize,
    };
    setFormState(newState);
    onFormDataChange(newState);
    if (enabled && prevSolarSize < MIN_SOLAR_SIZE) {
      setPrevSolarSize(MIN_SOLAR_SIZE);
    }
  };

  const handleToggleBattery = (enabled: boolean) => {
    setIsBatteryEnabled(enabled);
    const newMinBatterySize = batteryIncrement;
    const sizeToRestore =
      prevBatterySize >= newMinBatterySize
        ? prevBatterySize
        : newMinBatterySize;
    const newBatterySize = enabled ? sizeToRestore : 0;

    const newState = {
      ...formState,
      batterySize: newBatterySize,
    };
    setFormState(newState);
    onFormDataChange(newState);

    if (enabled && prevBatterySize < newMinBatterySize) {
      setPrevBatterySize(newMinBatterySize);
    }
  };

  const handleBatteryIncrementChange = (value: string) => {
    const increment = parseFloat(value);
    setBatteryIncrement(increment);

    if (isBatteryEnabled) {
      const newMin = increment;
      if (formState.batterySize < newMin) {
        const newState = {
          ...formState,
          batterySize: newMin,
        };
        setFormState(newState);
        onFormDataChange(newState);
        setPrevBatterySize(newMin);
      } else {
      }
    } else {
      if (prevBatterySize < increment) {
        setPrevBatterySize(increment);
      }
    }
  };

  const approximatePanelCount = useMemo(() => {
    if (!isSolarEnabled || formState.solarSize < AVERAGE_PANEL_KW) {
      return 0;
    }
    return Math.round(formState.solarSize / AVERAGE_PANEL_KW);
  }, [formState.solarSize, isSolarEnabled]);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-center">Calculator Inputs</CardTitle>
      </CardHeader>
      <CardContent>
        <TooltipProvider>
          <div className="space-y-6">
            <div className="text-sm font-semibold text-center text-muted-foreground pt-2">
              Toggle On/Off Core Features
            </div>
            <div className="flex flex-col space-y-3 sm:flex-row sm:space-y-0 sm:space-x-4 sm:justify-around items-center pb-4 border-b">
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant={isSolarEnabled ? "default" : "outline"}
                    onClick={() => handleToggleSolar(!isSolarEnabled)}
                    className={`cursor-pointer transition-colors w-full sm:w-24 h-14 md:h-16 flex flex-col items-center justify-center space-y-1 mb-4 md:mb-0 ${
                      isSolarEnabled
                        ? "bg-green-500 hover:bg-green-600 text-white"
                        : "text-muted-foreground"
                    }`}
                  >
                    <Sun className="h-6 w-6" />
                    <span className="text-xs font-medium">Solar</span>
                  </Button>
                </TooltipTrigger>
                <TooltipContent>
                  <p>Toggle Solar System {isSolarEnabled ? "Off" : "On"}</p>
                </TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant={isBatteryEnabled ? "default" : "outline"}
                    onClick={() => handleToggleBattery(!isBatteryEnabled)}
                    className={`cursor-pointer transition-colors w-full sm:w-24 h-14 md:h-16 flex flex-col items-center justify-center space-y-1 mb-4 md:mb-0 ${
                      isBatteryEnabled
                        ? "bg-blue-500 hover:bg-blue-600 text-white"
                        : "text-muted-foreground"
                    }`}
                  >
                    <BatteryCharging className="h-6 w-6" />
                    <span className="text-xs font-medium">Battery</span>
                  </Button>
                </TooltipTrigger>
                <TooltipContent>
                  <p>
                    Toggle Battery Storage {isBatteryEnabled ? "Off" : "On"}
                  </p>
                </TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant={formState.haveOrWillGetEv ? "default" : "outline"}
                    onClick={() =>
                      handleSwitchChange(
                        "haveOrWillGetEv",
                        !formState.haveOrWillGetEv
                      )
                    }
                    className={`cursor-pointer transition-colors w-full sm:w-24 h-14 md:h-16 flex flex-col items-center justify-center space-y-1 mb-4 md:mb-0  ${
                      formState.haveOrWillGetEv
                        ? "bg-purple-500 hover:bg-purple-600 text-white"
                        : "text-muted-foreground"
                    }`}
                  >
                    <Car className="h-6 w-6" />
                    <span className="text-xs font-medium">EV</span>
                  </Button>
                </TooltipTrigger>
                <TooltipContent>
                  <p>
                    {formState.haveOrWillGetEv ? "Disable" : "Enable"} EV
                    Presence
                  </p>
                  <p className="text-xs max-w-xs text-center pt-1">
                    Indicate if you have or plan to get an Electric Vehicle.
                  </p>
                </TooltipContent>
              </Tooltip>
            </div>

            <AnimatePresence>
              {isSolarEnabled && (
                <motion.div
                  key="solar-details"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: "auto" }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={{ duration: 0.3 }}
                  className="overflow-hidden"
                >
                  <div className="space-y-2 pt-4 mb-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-1">
                        <Label htmlFor="solar">Solar System Size (kWp)</Label>
                        {approximatePanelCount > 0 && (
                          <span className="text-xs text-muted-foreground ml-1">
                            (~{approximatePanelCount} panels)
                          </span>
                        )}
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
                              kilowatts (kWp). This determines how much energy
                              can be generated.
                            </p>
                          </TooltipContent>
                        </Tooltip>
                      </div>
                      <span className="text-sm font-medium text-muted-foreground">
                        {formState.solarSize.toFixed(1)} kWp
                      </span>
                    </div>
                    <Slider
                      id="solar"
                      min={MIN_SOLAR_SIZE}
                      max={MAX_SOLAR_SIZE}
                      step={solarSliderStep}
                      value={[formState.solarSize]}
                      onValueChange={(value) =>
                        handleSliderChange("solarSize", value)
                      }
                      disabled={!isSolarEnabled}
                      className="mt-2"
                    />
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            <AnimatePresence>
              {isBatteryEnabled && (
                <motion.div
                  key="battery-details"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: "auto" }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={{ duration: 0.3 }}
                  className="overflow-hidden"
                >
                  <div className="space-y-4 pt-4 mb-2">
                    <div className="space-y-2">
                      <div className="flex items-center space-x-1">
                        <Label>Battery Increment Size</Label>
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
                              Select the typical size increment for battery
                              modules (e.g., 2.5kWh or 3.6kWh blocks).
                            </p>
                          </TooltipContent>
                        </Tooltip>
                      </div>
                      <RadioGroup
                        defaultValue={String(batteryIncrement)}
                        onValueChange={handleBatteryIncrementChange}
                        className="flex space-x-4"
                      >
                        <div className="flex items-center space-x-2">
                          <RadioGroupItem value="2.5" id="inc-2.5" />
                          <Label htmlFor="inc-2.5">2.5 kWh</Label>
                        </div>
                        <div className="flex items-center space-x-2">
                          <RadioGroupItem value="3.6" id="inc-3.6" />
                          <Label htmlFor="inc-3.6">3.6 kWh</Label>
                        </div>
                      </RadioGroup>
                    </div>

                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
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
                                kilowatt-hours. Larger batteries store more
                                excess solar energy for later use.
                              </p>
                            </TooltipContent>
                          </Tooltip>
                        </div>
                        <span className="text-sm font-medium text-muted-foreground">
                          {formState.batterySize.toFixed(1)} kWh
                        </span>
                      </div>
                      <Slider
                        id="battery"
                        min={minBatterySize}
                        max={MAX_BATTERY_SIZE}
                        step={batterySliderStep}
                        value={[formState.batterySize]}
                        onValueChange={(value) =>
                          handleSliderChange("batterySize", value)
                        }
                        disabled={!isBatteryEnabled}
                        className="mt-2"
                      />
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

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
                  handleSelectChange(
                    "solarPanelDirection",
                    value as CardinalDirection
                  )
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

            <div className="space-y-2">
              <div className="flex items-center justify-between">
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
                        Your estimated total household electricity consumption
                        per year in kilowatt-hours. Check your utility bills for
                        accuracy.
                      </p>
                    </TooltipContent>
                  </Tooltip>
                </div>
                <span className="text-sm font-medium text-muted-foreground">
                  {formState.usage.toLocaleString()} kWh
                </span>
              </div>
              <Slider
                id="usage"
                min={MIN_USAGE}
                max={MAX_USAGE}
                step={100}
                value={[formState.usage]}
                onValueChange={(value) => handleSliderChange("usage", value)}
                className="mt-2"
              />
            </div>

            <div className="space-y-3 pt-4 border-t">
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
                        Being home during the day increases daytime energy
                        usage, allowing for more direct consumption of solar
                        power.
                      </p>
                    </TooltipContent>
                  </Tooltip>
                </div>
                <Switch
                  id="occupancy"
                  checked={formState.homeOccupancyDuringWorkHours}
                  onCheckedChange={(checked) =>
                    handleSwitchChange("homeOccupancyDuringWorkHours", checked)
                  }
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
                    handleSwitchChange("needFinance", checked)
                  }
                />
              </div>
            </div>
          </div>
        </TooltipProvider>
      </CardContent>
    </Card>
  );
}
