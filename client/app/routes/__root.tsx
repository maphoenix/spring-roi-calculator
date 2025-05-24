// app/routes/__root.tsx
import type { ReactNode } from "react";
import {
  Outlet,
  createRootRoute,
  HeadContent,
  Scripts,
  useSearch,
} from "@tanstack/react-router";
import "../styles/app.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NavigationBar } from "@/components/custom/navigation-bar";
import { Footer } from "@/components/custom/footer";

import appCss from "@/styles/app.css?url";

// Create a QueryClient instance
const queryClient = new QueryClient();

export const Route = createRootRoute({
  head: () => ({
    meta: [
      {
        charSet: "utf-8",
      },
      {
        name: "viewport",
        content: "width=device-width, initial-scale=1",
      },
      {
        title: "Green Energy ROI Calculator",
      },
    ],
    links: [
      {
        rel: "stylesheet",
        href: appCss,
      },
    ],
  }),
  component: RootComponent,
});

function RootComponent() {
  return (
    <RootDocument>
      <QueryClientProvider client={queryClient}>
        <LayoutWrapper>
          <Outlet />
        </LayoutWrapper>
      </QueryClientProvider>
    </RootDocument>
  );
}

function LayoutWrapper({ children }: { children: ReactNode }) {
  const searchParams = useSearch({ from: "__root__" });

  // Determine if we're in guide mode (no relevant search params) or dashboard mode (has search params)
  // This matches the logic in the IndexComponent
  const isInGuideMode = !Object.keys(searchParams).length;

  return (
    <div className="min-h-screen flex flex-col">
      {/* Show navigation only when not in guide mode */}
      {!isInGuideMode && <NavigationBar />}

      {/* Main content */}
      <main className={isInGuideMode ? "flex-1" : "flex-1"}>{children}</main>

      {/* Show footer only when not in guide mode */}
      {!isInGuideMode && <Footer />}
    </div>
  );
}

function RootDocument({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html>
      <head>
        <HeadContent />
      </head>
      <body>
        {children}
        <Scripts />
      </body>
    </html>
  );
}
