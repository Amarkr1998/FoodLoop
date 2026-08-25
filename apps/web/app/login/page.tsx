"use client";

import { useState } from "react";
import { signIn } from "next-auth/react";
import Link from "next/link";
import { Leaf } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export default function LoginPage() {
  const [isSigningIn, setIsSigningIn] = useState(false);

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="items-center text-center">
          <div className="mb-2 flex size-10 items-center justify-center rounded-lg bg-primary/10">
            <Leaf className="size-5 text-primary" />
          </div>
          <CardTitle className="text-xl">Sign in to FoodLoop</CardTitle>
          <CardDescription>Hyperlocal surplus-food redistribution, coordinated in real time.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Button
            className="w-full"
            disabled={isSigningIn}
            onClick={() => {
              // A second click before Keycloak's redirect lands overwrites
              // the first flow's state/PKCE cookies mid-flight, which
              // Keycloak then rejects as "State cookie was missing" on
              // whichever callback loses the race.
              setIsSigningIn(true);
              signIn("keycloak", { callbackUrl: "/dashboard" }, { prompt: "login" });
            }}
          >
            {isSigningIn ? "Redirecting…" : "Sign in"}
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            No account yet?{" "}
            <Link href="/register" className="font-medium text-primary hover:underline">
              Register here
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
