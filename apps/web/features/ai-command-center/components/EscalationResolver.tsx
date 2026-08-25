"use client";

import { useState } from "react";
import { AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { useResolveEscalation } from "../api";

export function EscalationResolver() {
  const [agentRunId, setAgentRunId] = useState("");
  const resolve = useResolveEscalation();

  return (
    <Card className="shadow-none">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <AlertTriangle className="size-4 text-warning" />
          Resolve escalation
        </CardTitle>
        <CardDescription>Approve or reject an agent run that escalated for human review.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-1.5">
          <Label htmlFor="agentRunId">Agent run ID</Label>
          <Input
            id="agentRunId"
            placeholder="UUID"
            value={agentRunId}
            onChange={(e) => {
              setAgentRunId(e.target.value);
              resolve.reset();
            }}
          />
        </div>
        <div className="flex gap-2">
          <Button
            size="sm"
            variant="outline"
            disabled={!agentRunId || resolve.isPending}
            onClick={() => resolve.mutate({ agentRunId, approve: false })}
          >
            Reject
          </Button>
          <Button
            size="sm"
            disabled={!agentRunId || resolve.isPending}
            onClick={() => resolve.mutate({ agentRunId, approve: true })}
          >
            Approve
          </Button>
        </div>

        {resolve.data && (
          <div className="rounded-md border bg-muted/30 p-3 text-sm">
            <div className="flex items-center justify-between">
              <span className="font-medium">{resolve.data.agentName ?? "Agent run"}</span>
              <Badge variant="secondary">{resolve.data.status ?? "Resolved"}</Badge>
            </div>
            {resolve.data.outcomeSummary && <p className="mt-1 text-muted-foreground">{resolve.data.outcomeSummary}</p>}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
