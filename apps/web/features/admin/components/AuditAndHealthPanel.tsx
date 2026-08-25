import { ScrollText, Activity } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { mockAuditLog, mockSystemHealth } from "../mock-data";

export function AuditAndHealthPanel() {
  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card className="shadow-none">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <ScrollText className="size-4" />
            Audit log
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {mockAuditLog.map((a) => (
            <div key={a.id} className="text-sm">
              <p>{a.action}</p>
              <p className="text-xs text-muted-foreground">{a.actor} · {a.when}</p>
            </div>
          ))}
        </CardContent>
      </Card>

      <Card className="shadow-none">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Activity className="size-4" />
            System health
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {mockSystemHealth.map((s) => (
            <div key={s.service} className="flex items-center justify-between text-sm">
              <span className="font-mono text-xs">{s.service}</span>
              <div className="flex items-center gap-2">
                <span className="text-xs text-muted-foreground">{s.latencyMs}ms</span>
                <Badge variant={s.status === "Healthy" ? "secondary" : "destructive"}>{s.status}</Badge>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
