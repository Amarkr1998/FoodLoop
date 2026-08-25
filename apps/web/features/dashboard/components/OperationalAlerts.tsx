import { AlertTriangle, AlertCircle, Info } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { mockOperationalAlerts, type OperationalAlert } from "../mock-data";

const severityConfig: Record<OperationalAlert["severity"], { icon: typeof AlertTriangle; className: string }> = {
  critical: { icon: AlertCircle, className: "text-destructive bg-destructive/10" },
  warning: { icon: AlertTriangle, className: "text-warning bg-warning/10" },
  info: { icon: Info, className: "text-info bg-info/10" },
};

export function OperationalAlerts() {
  return (
    <Card className="shadow-none">
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="text-base">Operational alerts</CardTitle>
        <Badge variant="outline" className="text-[10px] text-muted-foreground">
          Preview data
        </Badge>
      </CardHeader>
      <CardContent className="space-y-3">
        {mockOperationalAlerts.map((alert) => {
          const { icon: Icon, className } = severityConfig[alert.severity];
          return (
            <div key={alert.id} className="flex items-start gap-3 rounded-md border border-border p-3">
              <div className={cn("flex size-6 shrink-0 items-center justify-center rounded-md", className)}>
                <Icon className="size-3.5" />
              </div>
              <p className="text-sm leading-tight">{alert.message}</p>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
