"use client";

import { Bot, TriangleAlert } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { mockAgentStatus, mockRecentDecisions } from "../mock-data";

export function AgentStatusPanel() {
  return (
    <div className="space-y-4">
      <Card className="shadow-none">
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <CardTitle className="flex items-center gap-2 text-base">
            <Bot className="size-4" />
            Agent status
          </CardTitle>
          <Badge variant="outline" className="text-[10px] text-muted-foreground">Preview</Badge>
        </CardHeader>
        <CardContent className="space-y-2">
          {mockAgentStatus.map((a) => (
            <div key={a.name} className="flex items-center justify-between text-sm">
              <span>{a.name}</span>
              <span className="text-xs text-muted-foreground">{a.state} · {a.lastRunAgo}</span>
            </div>
          ))}
        </CardContent>
      </Card>

      <Card className="shadow-none">
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <CardTitle className="text-base">Recent decisions</CardTitle>
          <Badge variant="outline" className="text-[10px] text-muted-foreground">Preview</Badge>
        </CardHeader>
        <CardContent className="space-y-3">
          {mockRecentDecisions.map((d) => (
            <div key={d.id} className="text-sm">
              <div className="flex items-center gap-1.5">
                <span className="font-medium">{d.agent}</span>
                {d.escalated && <TriangleAlert className="size-3.5 text-warning" />}
              </div>
              <p className="text-muted-foreground">{d.summary}</p>
              <p className="text-xs text-muted-foreground">{d.when}</p>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
