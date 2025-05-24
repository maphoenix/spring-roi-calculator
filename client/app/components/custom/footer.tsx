import {
  Calculator,
  Mail,
  MapPin,
  Github,
  Twitter,
  Linkedin,
} from "lucide-react";

const Footer: React.FC = () => {
  return (
    <footer className="bg-muted/30 border-t">
      <div className="container py-12">
        <div className="grid gap-8 lg:grid-cols-2 md:grid-cols-1">
          {/* Company Info */}
          <div className="space-y-4 pl-4 md:pl-6">
            <div className="flex items-center space-x-2">
              <Calculator className="h-6 w-6 text-primary" />
              <span className="font-bold text-lg">Green Energy ROI</span>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Calculate your return on investment for solar panels, EV chargers,
              and other green energy solutions. Make informed decisions for a
              sustainable future.
            </p>
            <div className="flex space-x-4">
              <a
                href="#"
                className="text-muted-foreground hover:text-primary transition-colors"
                aria-label="Follow us on Twitter"
              >
                <Twitter className="h-5 w-5" />
              </a>
              <a
                href="#"
                className="text-muted-foreground hover:text-primary transition-colors"
                aria-label="Follow us on LinkedIn"
              >
                <Linkedin className="h-5 w-5" />
              </a>
              <a
                href="#"
                className="text-muted-foreground hover:text-primary transition-colors"
                aria-label="View our GitHub"
              >
                <Github className="h-5 w-5" />
              </a>
            </div>
          </div>

          {/* Quick Links - Commented out for later */}
          {/* <div className="space-y-4">
            <h3 className="font-semibold">Quick Links</h3>
            <ul className="space-y-2 text-sm">
              <li>
                <a
                  href="#calculator"
                  className="text-muted-foreground hover:text-primary transition-colors"
                >
                  ROI Calculator
                </a>
              </li>
              <li>
                <a
                  href="#guide"
                  className="text-muted-foreground hover:text-primary transition-colors"
                >
                  Solar Guide
                </a>
              </li>
              <li>
                <a
                  href="#benefits"
                  className="text-muted-foreground hover:text-primary transition-colors"
                >
                  Energy Benefits
                </a>
              </li>
              <li>
                <a
                  href="#financing"
                  className="text-muted-foreground hover:text-primary transition-colors"
                >
                  Financing Options
                </a>
              </li>
            </ul>
          </div> */}

          {/* Resources - Commented out for later */}
          {/* <div className="space-y-4">
            <h3 className="font-semibold">Resources</h3>
            <ul className="space-y-2 text-sm">
              <li>
                <a
                  href="#faq"
                  className="text-muted-foreground hover:text-primary transition-colors"
                >
                  FAQ
                </a>
              </li>
              <li>
                <a
                  href="#blog"
                  className="text-muted-foreground hover:text-primary transition-colors"
                >
                  Blog
                </a>
              </li>
              <li>
                <a
                  href="#case-studies"
                  className="text-muted-foreground hover:text-primary transition-colors"
                >
                  Case Studies
                </a>
              </li>
              <li>
                <a
                  href="#documentation"
                  className="text-muted-foreground hover:text-primary transition-colors"
                >
                  Documentation
                </a>
              </li>
            </ul>
          </div> */}

          {/* Contact */}
          <div className="space-y-4">
            <h3 className="font-semibold">Contact</h3>
            <ul className="space-y-3 text-sm">
              <li className="flex items-center space-x-2">
                <Mail className="h-4 w-4 text-muted-foreground" />
                <a
                  href="mailto:info@greenroi.com"
                  className="text-muted-foreground hover:text-primary transition-colors"
                >
                  info@greenroi.com
                </a>
              </li>
              <li className="flex items-start space-x-2">
                <MapPin className="h-4 w-4 text-muted-foreground mt-0.5" />
                <span className="text-muted-foreground">
                  London, United Kingdom
                </span>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Section */}
        <div className="border-t mt-8 pt-8 flex flex-col md:flex-row justify-between items-center space-y-4 md:space-y-0">
          <div className="text-sm text-muted-foreground pl-4 md:pl-6">
            © {new Date().getFullYear()} Green Energy ROI Calculator. All
            rights reserved.
          </div>
        </div>
      </div>
    </footer>
  );
};

export { Footer };
