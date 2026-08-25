import type { Metadata } from "next";
import type { ReactNode } from "react";
import "@foodloop/ui/src/tokens.css";
import "./globals.css";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "FoodLoop",
  description: "Hyperlocal surplus-food redistribution platform.",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body suppressHydrationWarning>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
