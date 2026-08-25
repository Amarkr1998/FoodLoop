"use client";

import { useState } from "react";
import { ScanSearch } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { FoodListingPicker } from "./FoodListingPicker";
import { useAnalyzeFoodListing } from "../api";

export function FoodAnalysisTrigger() {
  const [foodListingId, setFoodListingId] = useState("");
  const analyze = useAnalyzeFoodListing();

  return (
    <Card className="shadow-none">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <ScanSearch className="size-4 text-primary" />
          Food Intelligence Agent
        </CardTitle>
        <CardDescription>Analyze a listing for category, allergens, and safety flags.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-1.5">
          <Label>Food listing</Label>
          <FoodListingPicker
            value={foodListingId}
            onChange={(id) => {
              setFoodListingId(id);
              analyze.reset();
            }}
          />
        </div>
        <Button
          size="sm"
          disabled={!foodListingId || analyze.isPending}
          onClick={() => analyze.mutate(foodListingId)}
        >
          {analyze.isPending ? "Analyzing…" : "Analyze listing"}
        </Button>

        {analyze.data && (
          <div className="space-y-2 rounded-md border bg-muted/30 p-3 text-sm">
            <div className="flex items-center justify-between">
              <span className="font-medium">{analyze.data.status ?? "Completed"}</span>
              <div className="flex gap-1.5">
                {analyze.data.safety?.flagged && <Badge variant="destructive">Safety flag</Badge>}
                {analyze.data.escalated && <Badge variant="destructive">Escalated</Badge>}
              </div>
            </div>
            {analyze.data.analysis && (
              <div className="space-y-1 text-muted-foreground">
                {analyze.data.analysis.category && <p>Category: {analyze.data.analysis.category}</p>}
                {analyze.data.analysis.allergens && analyze.data.analysis.allergens.length > 0 && (
                  <p>Allergens: {analyze.data.analysis.allergens.join(", ")}</p>
                )}
                {analyze.data.analysis.estimatedServings !== undefined && (
                  <p>Estimated servings: {analyze.data.analysis.estimatedServings}</p>
                )}
                {analyze.data.analysis.missingInformation && analyze.data.analysis.missingInformation.length > 0 && (
                  <p>Missing info: {analyze.data.analysis.missingInformation.join(", ")}</p>
                )}
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
