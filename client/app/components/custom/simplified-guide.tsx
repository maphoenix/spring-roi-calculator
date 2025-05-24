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
import {
  ArrowLeft,
  Check,
  Compass,
  Home,
  Navigation,
  Car,
  Users,
  CreditCard,
  Sun,
  TreePine,
} from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";

// Add CSS for slow spinning animation
const compassStyles = `
  @keyframes spin-slow {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }
  .animate-spin-slow {
    animation: spin-slow 20s linear infinite;
  }
`;

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
  icon: React.ComponentType<{ className?: string }>;
}

// Define the steps and their corresponding questions/options
const steps: Step[] = [
  {
    id: "houseSize",
    title: "Your Home Size",
    question: "Roughly, how big is your home?",
    icon: Home,
    options: [
      {
        value: "small",
        label: "2 Bedrooms",
        image: "/2-bedroom-house.png",
        solarSize: 2.0,
        panelRangeText: "Up to 4 Panels",
      },
      {
        value: "medium",
        label: "3 Bedrooms",
        image: "/3-bedroom-house.png",
        solarSize: 3.2,
        panelRangeText: "5-8 Panels",
      },
      {
        value: "large",
        label: "4+ Bedrooms",
        image: "/4-bedroom-house.png",
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
    icon: Navigation,
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
    icon: Car,
    options: [
      {
        value: "yes",
        label: "Yes",
        image: "/ev-car-icon.png",
      },
      { value: "no", label: "No", image: "/petrol-car-icon.png" },
    ],
  },
  {
    id: "homeOccupancyDuringWorkHours",
    title: "Home Occupancy",
    question:
      "How many days per week is someone home during work hours (9am-5pm)?",
    icon: Users,
    options: [
      { value: "1", label: "1" },
      { value: "2", label: "2" },
      { value: "3", label: "3" },
      { value: "4", label: "4" },
      { value: "5", label: "5" },
    ],
  },
  {
    id: "needsFinance",
    title: "Payment Method",
    question: "How do you plan to pay for the system?",
    icon: CreditCard,
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
    north: { top: "-8%", left: "50%", transform: "translateX(-50%)" },
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
    south: { top: "108%", left: "50%", transform: "translate(-50%, -50%)" },
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
    <div className="flex flex-col items-center space-y-6 py-4">
      {/* Compass container with enhanced styling */}
      <div className="relative w-72 h-72 sm:w-80 sm:h-80 flex items-center justify-center my-4">
        {/* Outermost glow effect */}
        <div className="absolute inset-0 rounded-full bg-gradient-to-br from-purple-400/15 via-primary/10 to-violet-300/10 blur-2xl animate-pulse"></div>

        {/* Outer decorative circle with gradient border */}
        <div className="absolute inset-2 border-4 border-transparent bg-gradient-to-br from-purple-200/20 via-primary/15 to-violet-200/15 rounded-full pointer-events-none shadow-2xl">
          <div className="absolute inset-1 bg-gradient-to-br from-background/95 via-purple-50/20 to-background/80 rounded-full backdrop-blur-sm"></div>
        </div>

        {/* Middle circle with rotating border animation */}
        <div className="absolute inset-8 border-2 border-dashed border-primary/40 rounded-full pointer-events-none animate-spin-slow"></div>

        {/* Inner circle */}
        <div className="absolute inset-12 border-2 border-primary/50 rounded-full pointer-events-none bg-gradient-to-br from-background via-background/90 to-primary/5 shadow-inner"></div>

        {/* Decorative rays */}
        <div className="absolute inset-0 pointer-events-none">
          {[0, 45, 90, 135, 180, 225, 270, 315].map((angle) => (
            <div
              key={angle}
              className="absolute top-1/2 left-1/2 w-0.5 h-8 bg-gradient-to-t from-transparent via-primary/30 to-primary/60 origin-bottom"
              style={{
                transform: `translate(-50%, -100%) rotate(${angle}deg)`,
                transformOrigin: "bottom center",
              }}
            />
          ))}
        </div>

        {/* Center Yellow Solar Icon with pulsing animation */}
        <motion.div
          animate={{
            scale: [1, 1.1, 1],
            rotate: [0, 360],
          }}
          transition={{
            scale: { duration: 3, repeat: Infinity, ease: "easeInOut" },
            rotate: { duration: 20, repeat: Infinity, ease: "linear" },
          }}
          className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 text-yellow-500 bg-gradient-to-br from-yellow-100/30 via-yellow-200/20 to-yellow-300/10 rounded-full p-5 shadow-2xl border-3 border-yellow-400/30"
        >
          <Sun className="h-12 w-12 sm:h-14 sm:w-14 drop-shadow-lg" />
        </motion.div>

        {/* Direction Buttons */}
        {directionOptions.map((option) => {
          const pos = positions[option.value];
          const isSelected = selectedValue === option.value;
          if (!pos) return null; // Skip if position is not defined

          return (
            <Button
              key={option.value}
              variant={isSelected ? "default" : "outline"}
              size="sm"
              className={`cursor-pointer absolute transition-all duration-300 ease-in-out text-xs w-12 h-12 sm:w-14 sm:h-14 rounded-full flex items-center justify-center font-bold border-3 shadow-lg ${
                isSelected
                  ? "ring-4 ring-primary/30 ring-offset-4 ring-offset-background scale-125 shadow-2xl z-20 bg-black text-white border-black"
                  : "hover:scale-125 hover:shadow-2xl bg-black/90 backdrop-blur-sm border-black/80 hover:border-black text-white hover:bg-black hover:text-white shadow-lg"
              }`}
              style={{ top: pos.top, left: pos.left, transform: pos.transform }}
              onClick={() => onSelect(option.value)}
            >
              <span className="text-center leading-tight font-bold">
                {option.value === "north-east"
                  ? "NE"
                  : option.value === "north-west"
                    ? "NW"
                    : option.value === "south-east"
                      ? "SE"
                      : option.value === "south-west"
                        ? "SW"
                        : option.value === "north"
                          ? "N"
                          : option.value === "south"
                            ? "S"
                            : option.value === "east"
                              ? "E"
                              : option.value === "west"
                                ? "W"
                                : option.label}
              </span>
            </Button>
          );
        })}
      </div>

      {/* Don't Know Button */}
      {dontKnowOption && (
        <Button
          variant={selectedValue === "dont_know" ? "default" : "outline"}
          className={`cursor-pointer w-56 mt-12 pt-4 h-14 text-base font-semibold transition-all duration-300 ease-in-out border-3 shadow-lg ${
            selectedValue === "dont_know"
              ? "ring-4 ring-primary/30 ring-offset-4 ring-offset-background scale-105 shadow-2xl bg-gradient-to-r from-primary to-primary/80 text-white border-primary"
              : "hover:scale-105 hover:shadow-2xl bg-gradient-to-r from-background to-background/80 backdrop-blur-sm border-primary/60 hover:border-primary/80 hover:bg-primary/10 shadow-lg"
          }`}
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
    <>
      <style>{compassStyles}</style>
      <Card className="border-primary/30 overflow-hidden bg-gradient-to-br from-background via-purple-50/30 to-violet-100/20 shadow-2xl backdrop-blur-sm p-4 sm:p-6 relative">
        {/* Decorative gradient overlay */}
        <div className="absolute inset-0 bg-gradient-to-br from-purple-100/10 via-transparent to-violet-200/15 pointer-events-none"></div>
        <div className="relative z-10">
          <CardHeader className="pb-6">
            <div className="flex items-center space-x-3 mb-2">
              <div className="flex-shrink-0 w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                <step.icon className="h-5 w-5 text-primary" />
              </div>
              <div>
                <CardTitle className="text-xl text-primary">
                  {step.title}
                </CardTitle>
                <CardDescription className="text-sm">
                  Step {currentStep + 1} of {steps.length}
                </CardDescription>
              </div>
            </div>
            <div className="flex justify-between items-center pt-3">
              {currentStep > 0 ? (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleBack}
                  aria-label="Go back"
                  className="cursor-pointer hover:bg-primary/10 hover:scale-110 transition-all duration-200"
                >
                  <ArrowLeft className="h-5 w-5" />
                </Button>
              ) : (
                <div className="w-8 h-8"></div>
              )}
              <div className="text-sm font-medium text-muted-foreground">
                {progressPercentage.toFixed(0)}% Complete
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
              <CardContent className="space-y-6 p-4 sm:p-6 pt-2 min-h-[600px] flex flex-col justify-between">
                <div>
                  <p className="font-medium text-lg mb-6">{`${currentStep + 1}. ${step.question}`}</p>

                  {/* Add images for specific steps */}
                  {step.id === "homeOccupancyDuringWorkHours" && (
                    <div className="flex justify-center mb-6">
                      <img
                        src="/working-from-home-icon.png"
                        alt="Working from home illustration"
                        className="w-80 h-80 object-contain"
                      />
                    </div>
                  )}

                  {step.id === "needsFinance" && (
                    <div className="flex justify-center mb-6">
                      <img
                        src="/cash-vs-loan.png"
                        alt="Cash vs loan illustration"
                        className="w-96 h-80 object-contain"
                      />
                    </div>
                  )}

                  {/* Conditional Rendering for Compass or Buttons */}
                  {step.id === "roofDirection" ? (
                    <CompassSelector
                      options={step.options}
                      selectedValue={answers[step.id]}
                      onSelect={(value) => handleAnswer(step.id, value)}
                    />
                  ) : (
                    <div
                      className={`grid grid-cols-1 gap-4 ${
                        step.id === "houseSize"
                          ? "sm:grid-cols-2 lg:grid-cols-3"
                          : step.id === "homeOccupancyDuringWorkHours"
                            ? "sm:grid-cols-2 lg:grid-cols-5"
                            : "sm:grid-cols-3"
                      }`}
                    >
                      {step.options.map((option) => {
                        const isSelected = answers[step.id] === option.value;
                        return (
                          <Button
                            key={option.value}
                            variant={isSelected ? "default" : "outline"}
                            className={`cursor-pointer flex flex-col items-center justify-center text-center h-auto p-6 transition-all duration-200 ease-in-out relative overflow-hidden group border-3 shadow-lg ${
                              step.id === "houseSize" ? "min-h-[180px]" : ""
                            } ${
                              isSelected
                                ? "bg-primary text-primary-foreground ring-4 ring-primary/30 ring-offset-4 ring-offset-background scale-105 shadow-2xl border-primary"
                                : "hover:bg-primary/10 hover:border-primary/60 hover:scale-105 hover:shadow-xl border-border/70 bg-background/80 backdrop-blur-sm"
                            }`}
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
                              <motion.div
                                initial={{ scale: 0, rotate: -180 }}
                                animate={{ scale: 1, rotate: 0 }}
                                transition={{ duration: 0.3, ease: "backOut" }}
                                className="absolute -top-2 -right-2 bg-green-500 rounded-full p-2 z-10 shadow-lg border-2 border-background"
                              >
                                <Check className="h-4 w-4 text-white" />
                              </motion.div>
                            )}

                            {/* Conditional Rendering based on step.id and option type */}
                            {step.id === "houseSize" && "image" in option ? (
                              <>
                                <div
                                  className={`mb-3 relative flex justify-center items-center ${
                                    option.value === "small"
                                      ? "w-16 h-16"
                                      : option.value === "medium"
                                        ? "w-20 h-20"
                                        : option.value === "large"
                                          ? "w-24 h-24"
                                          : "w-20 h-20"
                                  }`}
                                >
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
                                    <span
                                      className={`text-xs mt-0.5 text-center ${
                                        isSelected
                                          ? "text-white"
                                          : "text-muted-foreground"
                                      }`}
                                    >
                                      {option.panelRangeText}
                                    </span>
                                  )}
                                {/* Check for solarSize safely before rendering */}
                                {"solarSize" in option &&
                                  option.solarSize !== undefined && (
                                    <span
                                      className={`text-xs mt-0.5 text-center ${
                                        isSelected
                                          ? "text-white"
                                          : "text-muted-foreground"
                                      }`}
                                    >
                                      Est. {option.solarSize} kWp Solar
                                    </span>
                                  )}
                              </>
                            ) : step.id === "hasEv" && "image" in option ? (
                              <>
                                <div className="mb-3 w-20 h-20 relative flex justify-center items-center">
                                  <img
                                    src={option.image}
                                    alt={`${option.label} EV car icon`}
                                    className="max-w-full max-h-full object-contain"
                                  />
                                </div>
                                <span className="font-medium mb-1 text-center">
                                  {option.label}
                                </span>
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

                <div className="flex justify-center items-center space-x-4 pt-6">
                  {steps.map((_, index) => (
                    <motion.div
                      key={index}
                      initial={{ scale: 0.8 }}
                      animate={{
                        scale:
                          index === currentStep
                            ? 1.2
                            : index <= currentStep
                              ? 1
                              : 0.8,
                        backgroundColor:
                          index <= currentStep
                            ? "var(--primary)"
                            : "var(--muted)",
                      }}
                      transition={{ duration: 0.3, ease: "easeInOut" }}
                      className={`h-3 w-3 rounded-full shadow-lg ${
                        index <= currentStep
                          ? "bg-primary ring-2 ring-primary/30"
                          : "bg-muted/60"
                      }`}
                    />
                  ))}
                </div>
              </CardContent>
            </motion.div>
          </AnimatePresence>
        </div>
      </Card>
    </>
  );
}
