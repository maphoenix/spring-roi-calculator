"use client";

import { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ExternalLink } from "lucide-react";

const affiliateLinks = [
  {
    name: "James' Link", // Your link
    url: "https://share.octopus.energy/sky-hare-157", // Updated URL
    description:
      "Switch to Octopus Energy using this link and you'll both get £50 credit!", // Slightly generic description
  },
  {
    name: "Dad's Link", // Your Dad's link
    url: "https://share.octopus.energy/happy-run-144", // Updated URL
    description:
      "Switch to Octopus Energy via this link and we both get £50 credit!", // Slightly generic description
  },
];

export function AffiliateBanner() {
  const [selectedLink, setSelectedLink] = useState(affiliateLinks[0]);

  useEffect(() => {
    // Select a link randomly on component mount (client-side)
    const randomIndex = Math.random() < 0.5 ? 0 : 1;
    setSelectedLink(affiliateLinks[randomIndex]);
  }, []);

  return (
    <Card className="bg-gradient-to-r from-purple-100 via-pink-100 to-red-100 border-purple-200 overflow-hidden">
      <CardHeader className="text-center pt-6 pb-3">
        <CardTitle className="text-xl font-bold text-purple-800">
          Switch & Save £50!
        </CardTitle>
        <CardDescription className="text-purple-700 text-sm px-4">
          Considering solar? Pair it with Octopus Energy's smart tariffs for
          maximum savings.
        </CardDescription>
      </CardHeader>
      <CardContent className="text-center space-y-4 pb-6 px-6">
        <p className="text-sm text-purple-600 italic">
          {selectedLink.description}
        </p>
        <Button
          asChild
          variant="default"
          className="bg-purple-600 hover:bg-purple-700 text-white shadow-md hover:shadow-lg transition-shadow duration-200"
        >
          <a href={selectedLink.url} target="_blank" rel="noopener noreferrer">
            Switch to Octopus Energy
            <ExternalLink className="ml-2 h-4 w-4" />
          </a>
        </Button>
      </CardContent>
    </Card>
  );
}
