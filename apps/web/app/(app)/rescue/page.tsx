"use client";

import { Timer } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";
import { useExpiringFood } from "@/features/dashboard/api";
import { RescueCard } from "@/features/rescue/components/RescueCard";

export default function RescuePage() {
  const { data, isLoading, isError } = useExpiringFood(240); // next 4 hours — the rescue-relevant window

  const sorted = [...(data ?? [])].sort((a, b) => {
    const ta = a.expiryTime ? new Date(a.expiryTime).getTime() : Infinity;
    const tb = b.expiryTime ? new Date(b.expiryTime).getTime() : Infinity;
    return ta - tb;
  });

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="flex items-center gap-2 text-xl font-semibold tracking-tight">
          <Timer className="size-5 text-destructive" />
          Expiry rescue center
        </h1>
        <p className="text-sm text-muted-foreground">
          Donations expiring within 4 hours, ranked by urgency — most critical first.
        </p>
      </div>

      <div className="space-y-3">
        {isLoading ? (
          <>
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
          </>
        ) : isError ? (
          <p className="py-12 text-center text-sm text-muted-foreground">Couldn&apos;t load rescue queue.</p>
        ) : sorted.length === 0 ? (
          <Card className="shadow-none">
            <CardContent className="py-12 text-center text-sm text-muted-foreground">
              Nothing urgent right now — no donations expiring within 4 hours.
            </CardContent>
          </Card>
        ) : (
          sorted.map((listing, i) => <RescueCard key={listing.id} listing={listing} priority={i} />)
        )}
      </div>
    </div>
  );
}
