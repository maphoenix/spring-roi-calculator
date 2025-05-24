"use client";

import * as React from "react";
import { Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";
import { Calculator, FileText, Menu, X } from "lucide-react";
import { useState } from "react";

const NavigationBar: React.FC = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-16 items-center justify-between">
        {/* Logo */}
        <div className="flex items-center ml-4 md:ml-6">
          <Link to="/" className="flex items-center space-x-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary/10">
              <Calculator className="h-6 w-6 text-primary" />
            </div>
            <div className="flex flex-col">
              <span className="font-bold text-lg leading-tight">
                Green Energy ROI
              </span>
              <span className="text-xs text-muted-foreground">Calculator</span>
            </div>
          </Link>
        </div>

        {/* Desktop Navigation */}
        <nav className="hidden md:flex items-center space-x-8">
          <Link
            to="/solar-roi"
            className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors"
          >
            Calculator
          </Link>
          <Link
            to="/"
            className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors"
          >
            About
          </Link>
        </nav>

        {/* CTA Button - Desktop */}
        <div className="hidden md:flex">
          <Button asChild className="shadow-sm">
            <Link to="/solar-roi">Get Started</Link>
          </Button>
        </div>

        {/* Mobile menu button */}
        <Button
          variant="ghost"
          size="sm"
          className="md:hidden"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
        >
          {mobileMenuOpen ? (
            <X className="h-5 w-5" />
          ) : (
            <Menu className="h-5 w-5" />
          )}
          <span className="sr-only">Toggle Menu</span>
        </Button>
      </div>

      {/* Mobile Navigation */}
      {mobileMenuOpen && (
        <div className="border-t md:hidden bg-background/95 backdrop-blur">
          <div className="container py-4">
            <nav className="flex flex-col space-y-4">
              <Link
                to="/solar-roi"
                className="flex items-center space-x-3 rounded-lg px-3 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground transition-colors"
                onClick={() => setMobileMenuOpen(false)}
              >
                <Calculator className="h-4 w-4" />
                <span>Calculator</span>
              </Link>
              <Link
                to="/"
                className="flex items-center space-x-3 rounded-lg px-3 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground transition-colors"
                onClick={() => setMobileMenuOpen(false)}
              >
                <FileText className="h-4 w-4" />
                <span>About</span>
              </Link>
              <div className="pt-2">
                <Button asChild className="w-full shadow-sm">
                  <Link
                    to="/solar-roi"
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    Get Started
                  </Link>
                </Button>
              </div>
            </nav>
          </div>
        </div>
      )}
    </header>
  );
};

export { NavigationBar };
