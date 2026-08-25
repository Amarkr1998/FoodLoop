"use client";

import { useState } from "react";
import Link from "next/link";
import { Sparkles, AlertOctagon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ExpiryCountdown } from "@/features/donations/components/ExpiryCountdown";
import type { FoodListing } from "@/features/donations/api";
import { useRunRescueCheck, type RescueCheckResponse } from "../api";

export function RescueCard({ listing, priority }: { listing: FoodListing; priority: number }) {
  const [result, setResult] = useState<RescueCheckResponse | null>(null);
  const runCheck = useRunRescueCheck();

  return (
    <Card className={priority === 0 ? "border-destructive/40 shadow-none" : "shadow-none"}>
      <CardContent className="space-y-3 px-5 py-4">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              {priority === 0 && <AlertOctagon className="size-4 shrink-0 text-destructive" />}
              <Link href={`/donations/${listing.id}`} className="truncate font-medium hover:underline">
                {listing.title}
              </Link>
            </div>
            <p className="mt-0.5 text-sm text-muted-foreground">
              {listing.quantityValue} {listing.quantityUnit} · {listing.foodCategory?.replace(/_/g, " ")}
            </p>
          </div>
          {listing.expiryTime && <ExpiryCountdown expiryTime={listing.expiryTime} />}
        </div>

        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant="outline"
            disabled={runCheck.isPending}
            onClick={() => {
              if (!listing.id) return;
              const msRemaining = listing.expiryTime ? new Date(listing.expiryTime).getTime() - Date.now() : Infinity;
              const threshold = msRemaining < 60 * 60_000 ? "T_MINUS_1H" : "T_MINUS_4H";
              runCheck.mutate({ foodListingId: listing.id, threshold }, { onSuccess: setResult });
            }}
          >
            <Sparkles className="size-3.5" />
            {runCheck.isPending ? "Running..." : "Run AI rescue check"}
          </Button>
          {result && (
            <>
              <Badge variant={result.escalated ? "destructive" : "secondary"}>{result.status}</Badge>
              {result.escalated && <Badge variant="outline">Escalated for human review</Badge>}
            </>
          )}
        </div>
        {result?.outcomeSummary && <p className="text-sm text-muted-foreground">{result.outcomeSummary}</p>}
      </CardContent>
    </Card>
  );
}
