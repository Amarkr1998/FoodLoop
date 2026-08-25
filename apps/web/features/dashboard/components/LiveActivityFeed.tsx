import { Package, GitMerge, Truck, Timer, Bot } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { mockActivityFeed, type ActivityEvent } from "../mock-data";

const iconFor: Record<ActivityEvent["kind"], typeof Package> = {
  donation_created: Package,
  match_created: GitMerge,
  pickup_completed: Truck,
  donation_expiring: Timer,
  ai_action: Bot,
};

export function LiveActivityFeed() {
  return (
    <Card className="shadow-none">
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="text-base">Live activity</CardTitle>
        <Badge variant="outline" className="text-[10px] text-muted-foreground">
          Preview — no event stream yet
        </Badge>
      </CardHeader>
      <CardContent className="space-y-4">
        {mockActivityFeed.map((event) => {
          const Icon = iconFor[event.kind];
          return (
            <div key={event.id} className="flex items-start gap-3">
              <div className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full bg-muted">
                <Icon className="size-3.5 text-muted-foreground" />
              </div>
              <div className="min-w-0">
                <p className="text-sm leading-tight">{event.message}</p>
                <p className="text-xs text-muted-foreground">{event.timestamp}</p>
              </div>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
