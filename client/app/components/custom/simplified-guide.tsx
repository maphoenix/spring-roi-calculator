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
import { ArrowRight, Check } from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";

// Define the steps and their corresponding questions/options
const steps = [
  {
    id: "houseSize",
    title: "Your Home Size",
    question: "Roughly, how big is your home?",
    options: [
      { value: "small", label: "Small (1-2 beds)" },
      { value: "medium", label: "Medium (3-4 beds)" },
      { value: "large", label: "Large (5+ beds)" },
    ],
  },
  {
    id: "roofDirection",
    title: "Solar Panel Direction",
    question: "Which direction does the main part of your roof face?",
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
  onComplete: (answers: Record<string, string>) => void;
}

export function SimplifiedGuide({ onComplete }: SimplifiedGuideProps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const navigate = useNavigate();

  const handleAnswer = (stepId: string, value: string) => {
    const newAnswers = { ...answers, [stepId]: value };
    setAnswers(newAnswers);

    if (currentStep < steps.length - 1) {
      setCurrentStep(currentStep + 1);
    } else {
      // Last step - call the onComplete callback instead of navigating directly
      console.log("Guide complete, calling onComplete with:", newAnswers);
      onComplete(newAnswers);
      // navigate({ to: '/', search: newAnswers, replace: true }); // Navigation handled by parent
    }
  };

  const step = steps[currentStep];

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
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {step.options.map((option) => (
                  <Button
                    key={option.value}
                    variant={
                      answers[step.id] === option.value ? "default" : "outline"
                    }
                    className={`justify-start text-left h-auto py-3 px-4 transition-all duration-150 ease-in-out ${answers[step.id] === option.value ? "ring-2 ring-primary ring-offset-2 ring-offset-background scale-105 shadow-lg" : "hover:bg-muted/50"}`}
                    onClick={() => handleAnswer(step.id, option.value)}
                  >
                    {answers[step.id] === option.value && (
                      <Check className="mr-2 h-5 w-5 text-primary-foreground" />
                    )}
                    {!(answers[step.id] === option.value) && (
                      <span className="mr-2 h-5 w-5"></span>
                    )}
                    <span className="flex-1">{option.label}</span>
                  </Button>
                ))}
              </div>
            </div>

            {/* Progress Indicator - slightly larger dots */}
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
