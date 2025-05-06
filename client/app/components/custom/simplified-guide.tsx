"use client";

import { useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Check, Compass } from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";

// Define interfaces for options and steps
interface BaseOption {
  value: string;
  label: string;
}

interface HouseSizeOption extends BaseOption {
  image: string;
  solarSize: number;
  panelRangeText: string;
}

interface StandardOption extends BaseOption {}

type StepOption = HouseSizeOption | StandardOption;

interface Step {
  id: string;
  title: string;
  question: string;
  options: StepOption[];
}

// Define the steps and their corresponding questions/options
const steps: Step[] = [
  {
    id: "houseSize",
    title: "Your Home Size",
    question: "Roughly, how big is your home?",
    options: [
      {
        value: "small",
        label: "2 Bedrooms",
        image: "/small-house.svg",
        solarSize: 2.0,
        panelRangeText: "Up to 4 Panels",
      },
      {
        value: "medium",
        label: "3 Bedrooms",
        image: "/medium-house.svg",
        solarSize: 3.2,
        panelRangeText: "5-8 Panels",
      },
      {
        value: "large",
        label: "4+ Bedrooms",
        image: "/large-house.svg",
        solarSize: 4.8,
        panelRangeText: "9+ Panels",
      },
    ],
  },
  {
    id: "roofDirection",
    title: "Solar Panel Direction",
    question:
      "Which direction will the solar panels primarily face on your roof?",
    options: [
      { value: "north", label: "North" },
      { value: "north-east", label: "North-East" },
      { value: "north-west", label: "North-West" },
      { value: "south", label: "South" },
      { value: "south-east", label: "South-East" },
      { value: "south-west", label: "South-West" },
      { value: "east", label: "East" },
      { value: "west", label: "West" },
      { value: "dont_know", label: "Don't Know" },
    ],
  },
  {
    id: "hasEv",
    title: "Electric Vehicle",
    question: "Do you own or plan to get an Electric Vehicle (EV)?",
    options: [
      { value: "yes", label: "Yes" },
      { value: "no", label: "No" },
    ],
  },
  {
    id: "isHome",
    title: "Home Occupancy",
    question: "Is someone usually home during weekday work hours (9am-5pm)?",
    options: [
      { value: "yes", label: "Yes" },
      { value: "no", label: "No" },
    ],
  },
  {
    id: "needsFinance",
    title: "Payment Method",
    question: "How do you plan to pay for the system?",
    options: [
      { value: "no", label: "Cash / Own Funds" }, // maps to needsFinance: no
      { value: "yes", label: "Finance / Loan" }, // maps to needsFinance: yes
    ],
  },
];

// Define props including the onComplete callback
interface SimplifiedGuideProps {
  onComplete: (answers: Record<string, string | number>) => void;
}

// *** NEW COMPONENT: CompassSelector ***
interface CompassSelectorProps {
  options: { value: string; label: string }[];
  selectedValue: string | number | undefined;
  onSelect: (value: string) => void;
}

const compassOrder: { [key: string]: number } = {
  "north-west": 0,
  north: 1,
  "north-east": 2,
  west: 3,
  center: 4,
  east: 5, // Added 'center' for placeholder
  "south-west": 6,
  south: 7,
  "south-east": 8,
};

const CompassSelector: React.FC<CompassSelectorProps> = ({
  options,
  selectedValue,
  onSelect,
}) => {
  const directionOptions = options.filter((opt) => opt.value !== "dont_know");
  const dontKnowOption = options.find((opt) => opt.value === "dont_know");

  // Map directions to angles for positioning
  const positions: {
    [key: string]: { top: string; left: string; transform: string };
  } = {
    north: { top: "0%", left: "50%", transform: "translateX(-50%)" },
    "north-east": {
      top: "15%",
      left: "85%",
      transform: "translate(-50%, -50%)",
    },
    east: {
      top: "50%",
      left: "95%",
      transform: "translateY(-50%) translateX(-50%)",
    },
    "south-east": {
      top: "85%",
      left: "85%",
      transform: "translate(-50%, -50%)",
    },
    south: { top: "100%", left: "50%", transform: "translate(-50%, -100%)" },
    "south-west": {
      top: "85%",
      left: "15%",
      transform: "translate(-50%, -50%)",
    },
    west: {
      top: "50%",
      left: "5%",
      transform: "translateY(-50%) translateX(-50%)",
    },
    "north-west": {
      top: "15%",
      left: "15%",
      transform: "translate(-50%, -50%)",
    },
  };

  return (
    <div className="flex flex-col items-center space-y-6">
      {/* Compass container */}
      <div className="relative w-56 h-56 sm:w-64 sm:h-64 flex items-center justify-center">
        {/* Add the circle border */}
        <div className="absolute inset-1 sm:inset-2 border border-border rounded-full pointer-events-none"></div>

        {/* Center Icon */}
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 text-muted-foreground">
          <Compass className="h-10 w-10 sm:h-12 sm:w-12" />
        </div>

        {/* Direction Buttons */}
        {directionOptions.map((option) => {
          const pos = positions[option.value];
          const isSelected = selectedValue === option.value;
          if (!pos) return null; // Skip if position is not defined

          return (
            <Button
              key={option.value}
              variant={isSelected ? "default" : "outline"}
              size="sm" // Smaller buttons for circular layout
              className={`absolute transition-all duration-150 ease-in-out text-xs sm:text-sm w-20 sm:w-24 ${
                isSelected
                  ? "ring-2 ring-primary ring-offset-2 ring-offset-background scale-110 shadow-lg z-10"
                  : "hover:scale-105"
              }`}
              style={{ top: pos.top, left: pos.left, transform: pos.transform }}
              onClick={() => onSelect(option.value)}
            >
              {option.label}
            </Button>
          );
        })}
      </div>

      {/* Don't Know Button */}
      {dontKnowOption && (
        <Button
          variant={selectedValue === "dont_know" ? "default" : "outline"}
          className={`w-48 mt-6 ${selectedValue === "dont_know" ? "ring-2 ring-primary ring-offset-2 ring-offset-background scale-105 shadow-lg" : ""}`}
          onClick={() => onSelect("dont_know")}
        >
          {dontKnowOption.label}
        </Button>
      )}
    </div>
  );
};

export function SimplifiedGuide({ onComplete }: SimplifiedGuideProps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string | number>>({});

  const handleAnswer = (stepId: string, value: string, solarSize?: number) => {
    const newAnswers: Record<string, string | number> = {
      ...answers,
      [stepId]: value,
    };

    if (stepId === "houseSize" && solarSize !== undefined) {
      newAnswers.estimatedSolarSize = solarSize;
    } else if (stepId === "houseSize") {
      // Ensure estimatedSolarSize is updated if changing houseSize
      const selectedOption = steps
        .find((s) => s.id === "houseSize")
        ?.options.find((o) => o.value === value);
      if (
        selectedOption &&
        "solarSize" in selectedOption &&
        selectedOption.solarSize !== undefined
      ) {
        newAnswers.estimatedSolarSize = selectedOption.solarSize;
      }
    }

    setAnswers(newAnswers);

    if (currentStep < steps.length - 1) {
      setCurrentStep(currentStep + 1);
    } else {
      console.log("Guide complete, calling onComplete with:", newAnswers);
      onComplete(newAnswers);
    }
  };

  const handleBack = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  };

  const step = steps[currentStep];
  const progressPercentage = ((currentStep + 1) / steps.length) * 100;

  // Add Framer Motion for step transitions
  return (
    <Card className="border-primary/50 overflow-hidden bg-gradient-to-br from-background to-muted/20 p-2 sm:p-4">
      <CardHeader className="pb-4">
        <CardTitle className="text-xl text-primary">
          Quick Estimate Guide
        </CardTitle>
        <CardDescription>
          Answer a few simple questions to get a starting estimate.
        </CardDescription>
        <div className="flex justify-between items-center pt-3">
          {currentStep > 0 ? (
            <Button
              variant="ghost"
              size="icon"
              onClick={handleBack}
              aria-label="Go back"
            >
              <ArrowLeft className="h-5 w-5" />
            </Button>
          ) : (
            <div className="w-8 h-8"></div>
          )}
          <div className="text-sm font-medium text-muted-foreground">
            Step {currentStep + 1} of {steps.length} (
            {progressPercentage.toFixed(0)}%)
          </div>
          <div className="w-8 h-8"></div>
        </div>
      </CardHeader>
      <AnimatePresence mode="wait">
        <motion.div
          key={currentStep}
          initial={{ opacity: 0, x: 50 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: -50 }}
          transition={{ duration: 0.3 }}
        >
          <CardContent className="space-y-6 p-4 sm:p-6 pt-2">
            <div>
              <p className="font-medium text-lg mb-6">{`${currentStep + 1}. ${step.question}`}</p>
              {/* Conditional Rendering for Compass or Buttons */}
              {step.id === "roofDirection" ? (
                <CompassSelector
                  options={step.options}
                  selectedValue={answers[step.id]}
                  onSelect={(value) => handleAnswer(step.id, value)}
                />
              ) : (
                <div
                  className={`grid grid-cols-1 gap-4 ${step.id === "houseSize" ? "sm:grid-cols-2 lg:grid-cols-3" : "sm:grid-cols-3"}`}
                >
                  {step.options.map((option) => {
                    const isSelected = answers[step.id] === option.value;
                    return (
                      <Button
                        key={option.value}
                        variant={isSelected ? "default" : "outline"}
                        className={`flex flex-col items-center justify-center text-center h-auto p-4 transition-all duration-150 ease-in-out relative overflow-hidden
                        ${isSelected ? "bg-accent text-accent-foreground ring-2 ring-primary ring-offset-2 ring-offset-background scale-105 shadow-lg hover:bg-accent hover:text-accent-foreground" : "hover:bg-accent"}`}
                        onClick={() =>
                          handleAnswer(
                            step.id,
                            option.value,
                            // Access solarSize safely after type check
                            step.id === "houseSize" && "solarSize" in option
                              ? option.solarSize
                              : undefined
                          )
                        }
                      >
                        {answers[step.id] === option.value && (
                          <div className="absolute top-2 right-2 bg-primary rounded-full p-1 z-10">
                            <Check className="h-4 w-4 text-primary-foreground" />
                          </div>
                        )}

                        {/* Conditional Rendering based on step.id and option type */}
                        {step.id === "houseSize" && "image" in option ? (
                          <>
                            <div className="mb-3 w-20 h-20 relative flex justify-center items-center">
                              {/* Access image safely */}
                              <img
                                src={option.image}
                                alt={`${option.label} house illustration`}
                                className="max-w-full max-h-full object-contain"
                              />
                            </div>
                            <span className="font-medium mb-1 text-center">
                              {option.label}
                            </span>
                            {/* Access panelRangeText safely */}
                            {"panelRangeText" in option &&
                              option.panelRangeText && (
                                <span className="text-xs text-muted-foreground mt-0.5 text-center">
                                  {option.panelRangeText}
                                </span>
                              )}
                            {/* Check for solarSize safely before rendering */}
                            {"solarSize" in option &&
                              option.solarSize !== undefined && (
                                <span className="text-xs text-muted-foreground mt-0.5 text-center">
                                  Est. {option.solarSize} kWp Solar
                                </span>
                              )}
                          </>
                        ) : (
                          <span className="flex-1 font-medium flex items-center justify-center">
                            {option.label}
                          </span>
                        )}
                      </Button>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="flex justify-center items-center space-x-3 pt-4">
              {steps.map((_, index) => (
                <div
                  key={index}
                  className={`h-2.5 w-2.5 rounded-full transition-colors duration-300 ${index <= currentStep ? "bg-primary" : "bg-muted"}`}
                />
              ))}
            </div>
          </CardContent>
        </motion.div>
      </AnimatePresence>
    </Card>
  );
}
