import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const STATUS_STYLES: Record<string, string> = {
  DRAFT: "bg-muted text-muted-foreground border-transparent",
  AVAILABLE: "bg-success/10 text-success border-success/20",
  CLAIMED: "bg-info/10 text-info border-info/20",
  IN_TRANSIT: "bg-info/10 text-info border-info/20",
  COMPLETED: "bg-primary/10 text-primary border-primary/20",
  EXPIRED: "bg-destructive/10 text-destructive border-destructive/20",
  CANCELLED: "bg-muted text-muted-foreground border-transparent line-through",
};

export function DonationStatusBadge({ status }: { status: string }) {
  return (
    <Badge variant="outline" className={cn("font-medium", STATUS_STYLES[status] ?? "")}>
      {status.replace(/_/g, " ")}
    </Badge>
  );
}
