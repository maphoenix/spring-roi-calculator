import { createFileRoute, Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Calculator,
  Zap,
  TrendingUp,
  Leaf,
  ArrowRight,
  Star,
  Users,
  Shield,
} from "lucide-react";
import { motion } from "framer-motion";

export const Route = createFileRoute("/")({
  component: HomePage,
});

function HomePage() {
  return (
    <div className="bg-gradient-to-br from-background via-purple-50/10 to-violet-50/15 min-h-screen">
      {/* Hero Section */}
      <section className="container mx-auto px-4 py-16 md:py-24">
        <div className="text-center max-w-4xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
          >
            <div className="flex items-center justify-center mb-6">
              <div className="flex items-center justify-center w-16 h-16 rounded-full bg-primary/10 mr-4">
                <Calculator className="h-8 w-8 text-primary" />
              </div>
              <h1 className="text-4xl md:text-6xl font-bold bg-gradient-to-r from-purple-600 to-blue-600 bg-clip-text text-transparent">
                Free Solar ROI Calculator
              </h1>
            </div>

            <p className="text-xl md:text-2xl text-muted-foreground mb-8 leading-relaxed">
              Calculate your potential savings, payback period, and return on
              investment for solar panels in seconds.
              <span className="text-green-600 font-semibold block mt-2">
                100% Free • No Email Required • Instant Results
              </span>
            </p>

            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-12">
              <Button
                asChild
                size="lg"
                className="text-lg px-8 py-6 shadow-lg hover:shadow-xl transition-all"
              >
                <Link to="/solar-roi">
                  <Calculator className="mr-2 h-5 w-5" />
                  Calculate My Savings
                  <ArrowRight className="ml-2 h-5 w-5" />
                </Link>
              </Button>

              <div className="flex items-center text-sm text-muted-foreground">
                <Star className="h-4 w-4 text-yellow-500 mr-1" />
                <span>Trusted by 10,000+ homeowners</span>
              </div>
            </div>
          </motion.div>
        </div>

        {/* Feature Cards */}
        <motion.div
          initial={{ opacity: 0, y: 40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-6xl mx-auto"
        >
          <Card className="text-center hover:shadow-lg transition-shadow">
            <CardHeader>
              <div className="mx-auto w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mb-4">
                <TrendingUp className="h-6 w-6 text-green-600" />
              </div>
              <CardTitle>Instant ROI Analysis</CardTitle>
              <CardDescription>
                Get detailed payback period and return on investment
                calculations in real-time
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="text-center hover:shadow-lg transition-shadow">
            <CardHeader>
              <div className="mx-auto w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mb-4">
                <Zap className="h-6 w-6 text-blue-600" />
              </div>
              <CardTitle>Accurate Estimates</CardTitle>
              <CardDescription>
                Based on real energy prices, solar costs, and your specific
                usage patterns
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="text-center hover:shadow-lg transition-shadow">
            <CardHeader>
              <div className="mx-auto w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center mb-4">
                <Leaf className="h-6 w-6 text-purple-600" />
              </div>
              <CardTitle>Environmental Impact</CardTitle>
              <CardDescription>
                See how much CO₂ you'll save and your positive impact on the
                environment
              </CardDescription>
            </CardHeader>
          </Card>
        </motion.div>
      </section>

      {/* Benefits Section */}
      <section className="bg-muted/30 py-16">
        <div className="container mx-auto px-4">
          <div className="text-center mb-12">
            <h2 className="text-3xl md:text-4xl font-bold mb-4">
              Why Use Our Solar Calculator?
            </h2>
            <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
              Make informed decisions about your solar investment with our
              comprehensive analysis
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 max-w-6xl mx-auto">
            <div className="text-center">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Calculator className="h-8 w-8 text-green-600" />
              </div>
              <h3 className="font-semibold mb-2">Detailed Analysis</h3>
              <p className="text-sm text-muted-foreground">
                Get comprehensive breakdowns of costs, savings, and payback
                periods
              </p>
            </div>

            <div className="text-center">
              <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Users className="h-8 w-8 text-blue-600" />
              </div>
              <h3 className="font-semibold mb-2">Easy to Use</h3>
              <p className="text-sm text-muted-foreground">
                Simple interface guides you through the process step by step
              </p>
            </div>

            <div className="text-center">
              <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Shield className="h-8 w-8 text-purple-600" />
              </div>
              <h3 className="font-semibold mb-2">No Personal Info</h3>
              <p className="text-sm text-muted-foreground">
                Calculate your savings without providing email or personal
                details
              </p>
            </div>

            <div className="text-center">
              <div className="w-16 h-16 bg-orange-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Zap className="h-8 w-8 text-orange-600" />
              </div>
              <h3 className="font-semibold mb-2">Instant Results</h3>
              <p className="text-sm text-muted-foreground">
                Get your solar ROI analysis in seconds, not days
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16">
        <div className="container mx-auto px-4 text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.4 }}
            className="max-w-3xl mx-auto"
          >
            <h2 className="text-3xl md:text-4xl font-bold mb-6">
              Ready to Calculate Your Solar Savings?
            </h2>
            <p className="text-xl text-muted-foreground mb-8">
              Join thousands of homeowners who have already discovered their
              solar potential
            </p>

            <div className="bg-gradient-to-r from-green-50 to-blue-50 p-8 rounded-lg border border-green-200 mb-8">
              <div className="flex items-center justify-center mb-4">
                <Leaf className="h-8 w-8 text-green-600 mr-2" />
                <span className="text-2xl font-bold text-green-800">
                  Save 100 trees per year
                </span>
              </div>
              <p className="text-green-700">
                Every person that switches to solar saves approximately 100
                trees per year through reduced carbon emissions
              </p>
            </div>

            <Button
              asChild
              size="lg"
              className="text-lg px-8 py-6 shadow-lg hover:shadow-xl transition-all"
            >
              <Link to="/solar-roi">
                Start Your Free Calculation
                <ArrowRight className="ml-2 h-5 w-5" />
              </Link>
            </Button>

            <p className="text-sm text-muted-foreground mt-4">
              Takes less than 2 minutes • No signup required
            </p>
          </motion.div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border/40 py-8">
        <div className="container mx-auto px-4 text-center">
          <p className="text-sm text-muted-foreground">
            Free Solar ROI Calculator - Help homeowners make informed decisions
            about solar energy
          </p>
        </div>
      </footer>
    </div>
  );
}
