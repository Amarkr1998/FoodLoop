"use client";

import Link from "next/link";
import { Timer } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { useExpiringFood } from "../api";

export function ExpiringFoodPanel() {
  const { data, isLoading, isError } = useExpiringFood(1440);
  const listings = data ?? [];

  return (
    <Card className="shadow-none">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Timer className="size-4 text-warning" />
          Expiring within 24h
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        {isLoading ? (
          <>
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </>
        ) : isError ? (
          <p className="py-6 text-center text-sm text-muted-foreground">Couldn&apos;t load expiring listings.</p>
        ) : listings.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">Nothing expiring soon.</p>
        ) : (
          listings.slice(0, 6).map((listing) => (
            <Link
              key={listing.id}
              href={`/donations/${listing.id}`}
              className="flex items-center justify-between gap-3 rounded-md border border-border px-3 py-2 text-sm hover:bg-muted"
            >
              <span className="truncate font-medium">{listing.title}</span>
              <Badge variant="outline" className="shrink-0 border-warning/40 text-warning">
                {listing.expiryTime ? new Date(listing.expiryTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—"}
              </Badge>
            </Link>
          ))
        )}
      </CardContent>
    </Card>
  );
}
