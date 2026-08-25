import type { ReactNode } from "react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export function KanbanColumn({
  title,
  count,
  accent,
  preview,
  children,
}: {
  title: string;
  count: number;
  accent?: "warning" | "destructive" | "info" | "success";
  preview?: boolean;
  children: ReactNode;
}) {
  return (
    <div className="flex min-w-0 flex-1 flex-col rounded-lg border border-border bg-muted/30">
      <div className="flex items-center justify-between border-b border-border px-3 py-2.5">
        <div className="flex items-center gap-2">
          <span
            className={cn(
              "size-2 rounded-full",
              accent === "warning" && "bg-warning",
              accent === "destructive" && "bg-destructive",
              accent === "info" && "bg-info",
              accent === "success" && "bg-success",
              !accent && "bg-muted-foreground",
            )}
          />
          <span className="text-sm font-medium">{title}</span>
          <Badge variant="secondary" className="h-5 min-w-5 justify-center px-1 text-xs">
            {count}
          </Badge>
        </div>
        {preview && (
          <Badge variant="outline" className="text-[10px] text-muted-foreground">
            Preview
          </Badge>
        )}
      </div>
      <div className="flex-1 space-y-2 overflow-y-auto p-2">{children}</div>
    </div>
  );
}
