import type { LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface MetricCardProps {
  label: string;
  value: string | number;
  icon: LucideIcon;
  accent?: "primary" | "success" | "warning" | "destructive" | "info";
  loading?: boolean;
  hint?: string;
}

const accentClasses: Record<NonNullable<MetricCardProps["accent"]>, string> = {
  primary: "bg-primary/10 text-primary",
  success: "bg-success/10 text-success",
  warning: "bg-warning/10 text-warning",
  destructive: "bg-destructive/10 text-destructive",
  info: "bg-info/10 text-info",
};

export function MetricCard({ label, value, icon: Icon, accent = "primary", loading, hint }: MetricCardProps) {
  return (
    <Card className="shadow-none">
      <CardContent className="flex items-start justify-between gap-3 px-5 py-4">
        <div className="min-w-0">
          <p className="text-sm font-medium text-muted-foreground">{label}</p>
          {loading ? (
            <Skeleton className="mt-1.5 h-7 w-20" />
          ) : (
            <p className="mt-1 text-2xl font-semibold tracking-tight">{value}</p>
          )}
          {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
        </div>
        <div className={cn("flex size-9 shrink-0 items-center justify-center rounded-lg", accentClasses[accent])}>
          <Icon className="size-4.5" />
        </div>
      </CardContent>
    </Card>
  );
}
