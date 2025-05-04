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
import { ArrowLeft, ArrowRight, Check } from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";

// Define the steps and their corresponding questions/options
const steps = [
  {
    id: "houseSize",
    title: "Your Home Size",
    question: "Roughly, how big is your home?",
    options: [
      {
        value: "small",
        label: "Small (Up to 4)",
        image: "/small-house.svg",
        solarSize: 1.6,
        panelRangeText: "Up to 4 Panels",
      },
      {
        value: "medium",
        label: "Medium (5-8)",
        image: "/medium-house.svg",
        solarSize: 3.2,
        panelRangeText: "5-8 Panels",
      },
      {
        value: "large",
        label: "Large (9+)",
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

export function SimplifiedGuide({ onComplete }: SimplifiedGuideProps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string | number>>({});
  const navigate = useNavigate();

  const handleAnswer = (stepId: string, value: string, solarSize?: number) => {
    const newAnswers: Record<string, string | number> = {
      ...answers,
      [stepId]: value,
    };

    if (stepId === "houseSize" && solarSize !== undefined) {
      newAnswers.estimatedSolarSize = solarSize;
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
              <p className="font-medium text-lg mb-4">{`${currentStep + 1}. ${step.question}`}</p>
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
                          step.id === "houseSize" ? option.solarSize : undefined
                        )
                      }
                    >
                      {answers[step.id] === option.value && (
                        <div className="absolute top-2 right-2 bg-primary rounded-full p-1 z-10">
                          <Check className="h-4 w-4 text-primary-foreground" />
                        </div>
                      )}

                      {step.id === "houseSize" ? (
                        <>
                          <div className="mb-3 w-20 h-20 relative flex justify-center items-center">
                            <img
                              src={option.image}
                              alt={`${option.label} house illustration`}
                              className="max-w-full max-h-full object-contain"
                            />
                          </div>
                          <span className="font-medium mb-1 text-center">
                            {option.label}
                          </span>
                          {option.panelRangeText && (
                            <span className="text-xs text-muted-foreground mt-0.5 text-center">
                              {option.panelRangeText}
                            </span>
                          )}
                          {option.solarSize !== undefined && (
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
