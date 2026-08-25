"use client";

import { useState } from "react";
import { Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { FoodListingPicker } from "./FoodListingPicker";
import { useSuggestMatch } from "../api";

export function MatchSuggestionTrigger() {
  const [foodListingId, setFoodListingId] = useState("");
  const suggest = useSuggestMatch();

  return (
    <Card className="shadow-none">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Sparkles className="size-4 text-primary" />
          Matching Agent
        </CardTitle>
        <CardDescription>Suggest the best receiver match for a donation.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-1.5">
          <Label>Food listing</Label>
          <FoodListingPicker
            value={foodListingId}
            onChange={(id) => {
              setFoodListingId(id);
              suggest.reset();
            }}
          />
        </div>
        <Button
          size="sm"
          disabled={!foodListingId || suggest.isPending}
          onClick={() => suggest.mutate(foodListingId)}
        >
          {suggest.isPending ? "Suggesting…" : "Suggest match"}
        </Button>

        {suggest.data && (
          <div className="rounded-md border bg-muted/30 p-3 text-sm">
            <div className="flex items-center justify-between">
              <span className="font-medium">{suggest.data.status ?? "Completed"}</span>
              {suggest.data.escalated && <Badge variant="destructive">Escalated</Badge>}
            </div>
            {suggest.data.proposal && (
              <p className="mt-1 text-muted-foreground">
                Score {suggest.data.proposal.score?.toFixed(2) ?? "—"} ·{" "}
                {suggest.data.proposal.distanceMeters !== undefined
                  ? `${(suggest.data.proposal.distanceMeters / 1000).toFixed(1)} km away`
                  : "distance unknown"}
              </p>
            )}
            {suggest.data.proposal?.aiRationale && (
              <p className="mt-1 italic text-muted-foreground">&ldquo;{suggest.data.proposal.aiRationale}&rdquo;</p>
            )}
            {suggest.data.outcomeSummary && <p className="mt-1 text-muted-foreground">{suggest.data.outcomeSummary}</p>}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
