import { Clock, MapPin } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import type { PickupTask } from "../api";

export function PickupCard({
  task,
  actionLabel,
  onAction,
  pending,
}: {
  task: PickupTask;
  actionLabel?: string;
  onAction?: () => void;
  pending?: boolean;
}) {
  return (
    <Card className="shadow-none">
      <CardContent className="space-y-2 px-4 py-3">
        <p className="text-xs font-mono text-muted-foreground">#{task.id?.slice(0, 8)}</p>
        {task.scheduledWindowStart && task.scheduledWindowEnd && (
          <p className="flex items-center gap-1.5 text-sm">
            <Clock className="size-3.5 text-muted-foreground" />
            {new Date(task.scheduledWindowStart).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })} –{" "}
            {new Date(task.scheduledWindowEnd).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
          </p>
        )}
        {task.latitude && task.longitude && (
          <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <MapPin className="size-3.5" />
            {task.latitude.toFixed(3)}, {task.longitude.toFixed(3)}
          </p>
        )}
        {actionLabel && onAction && (
          <Button size="sm" className="w-full" disabled={pending} onClick={onAction}>
            {actionLabel}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
