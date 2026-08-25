"use client";

import { Sparkles, MapPin } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useMatchCandidates, useAcceptMatchCandidate } from "../api";

function scoreBadgeVariant(score?: number): "default" | "secondary" | "outline" {
  if (score === undefined) return "outline";
  if (score >= 0.75) return "default";
  if (score >= 0.5) return "secondary";
  return "outline";
}

export function MatchCandidatesPanel({ foodListingId }: { foodListingId: string }) {
  const { data: candidates, isLoading, isError } = useMatchCandidates(foodListingId);
  const acceptCandidate = useAcceptMatchCandidate();

  return (
    <Card className="shadow-none">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Sparkles className="size-4 text-primary" />
          Recommended receivers
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        {isLoading ? (
          <>
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </>
        ) : isError ? (
          <p className="py-6 text-center text-sm text-muted-foreground">Couldn&apos;t load candidates.</p>
        ) : !candidates || candidates.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">No nearby receivers found within range.</p>
        ) : (
          candidates.map((c) => (
            <div key={c.receiverOrgId} className="flex items-center justify-between gap-3 rounded-md border border-border px-3 py-2.5">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{c.receiverOrgName ?? "Unknown receiver"}</p>
                <p className="flex items-center gap-1 text-xs text-muted-foreground">
                  <MapPin className="size-3" />
                  {c.distanceMeters !== undefined ? `${(c.distanceMeters / 1000).toFixed(1)} km away` : "Distance unknown"}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                {typeof c.score === "number" && (
                  <Badge variant={scoreBadgeVariant(c.score)}>{Math.round(c.score * 100)}% match</Badge>
                )}
                <Button
                  size="sm"
                  disabled={acceptCandidate.isPending}
                  onClick={() =>
                    c.receiverOrgId &&
                    acceptCandidate.mutate({ foodListingId, receiverOrgId: c.receiverOrgId })
                  }
                >
                  Accept
                </Button>
              </div>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}
