"use client";

import { useState } from "react";
import { ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { useAssessTrustRisk } from "../api";

export function TrustAssessmentTrigger() {
  const [targetUserId, setTargetUserId] = useState("");
  const assess = useAssessTrustRisk();

  return (
    <Card className="shadow-none">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <ShieldCheck className="size-4 text-primary" />
          Trust &amp; Safety Agent
        </CardTitle>
        <CardDescription>Run a risk assessment for a user account.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-1.5">
          <Label htmlFor="targetUserId">Target user ID</Label>
          <Input
            id="targetUserId"
            placeholder="UUID"
            value={targetUserId}
            onChange={(e) => {
              setTargetUserId(e.target.value);
              assess.reset();
            }}
          />
        </div>
        <Button
          size="sm"
          disabled={!targetUserId || assess.isPending}
          onClick={() => assess.mutate(targetUserId)}
        >
          {assess.isPending ? "Assessing…" : "Run assessment"}
        </Button>

        {assess.data && (
          <div className="rounded-md border bg-muted/30 p-3 text-sm">
            <div className="flex items-center justify-between">
              <span className="font-medium">{assess.data.status ?? "Completed"}</span>
              {assess.data.escalated && <Badge variant="destructive">Escalated</Badge>}
            </div>
            {assess.data.outcomeSummary && (
              <p className="mt-1 text-muted-foreground">{assess.data.outcomeSummary}</p>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
