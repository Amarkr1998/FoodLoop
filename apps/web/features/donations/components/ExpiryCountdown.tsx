"use client";

import { useEffect, useState } from "react";
import { Clock } from "lucide-react";
import { cn } from "@/lib/utils";

function formatRemaining(ms: number): string {
  if (ms <= 0) return "Expired";
  const totalMinutes = Math.floor(ms / 60_000);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours >= 24) return `${Math.floor(hours / 24)}d ${hours % 24}h`;
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
}

export function ExpiryCountdown({ expiryTime }: { expiryTime: string }) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), 30_000);
    return () => clearInterval(interval);
  }, []);

  const remainingMs = new Date(expiryTime).getTime() - now;
  const severity = remainingMs <= 0 ? "expired" : remainingMs < 60 * 60_000 ? "critical" : remainingMs < 4 * 60 * 60_000 ? "warning" : "normal";

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 text-sm font-medium tabular-nums",
        severity === "expired" && "text-muted-foreground line-through",
        severity === "critical" && "text-destructive",
        severity === "warning" && "text-warning",
        severity === "normal" && "text-muted-foreground",
      )}
    >
      <Clock className="size-3.5" />
      {formatRemaining(remainingMs)}
    </span>
  );
}
