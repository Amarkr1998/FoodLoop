"use client";

import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useCommunityImpactTrend } from "../api";

export function ImpactTrendChart() {
  const { data, isLoading, isError } = useCommunityImpactTrend();

  const rows = (data ?? []).map((m) => ({
    month: m.month ? new Date(m.month).toLocaleDateString(undefined, { month: "short" }) : "",
    kgSaved: m.estimatedKgSaved ?? 0,
  }));

  return (
    <Card className="shadow-none">
      <CardHeader>
        <CardTitle className="text-base">Food rescued over time</CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-64 w-full" />
        ) : isError ? (
          <p className="flex h-64 items-center justify-center text-sm text-muted-foreground">
            Couldn&apos;t load trend data.
          </p>
        ) : rows.length === 0 ? (
          <p className="flex h-64 items-center justify-center text-sm text-muted-foreground">
            No rescues recorded yet.
          </p>
        ) : (
          <ResponsiveContainer width="100%" height={256}>
            <AreaChart data={rows} margin={{ left: -20 }}>
              <defs>
                <linearGradient id="kgSavedFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="var(--color-primary)" stopOpacity={0.25} />
                  <stop offset="95%" stopColor="var(--color-primary)" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-border)" />
              <XAxis dataKey="month" fontSize={12} tickLine={false} axisLine={false} />
              <YAxis fontSize={12} tickLine={false} axisLine={false} width={40} />
              <Tooltip
                contentStyle={{
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  fontSize: 12,
                }}
                formatter={(value) => [`${Number(value ?? 0).toFixed(1)} kg`, "Food saved"]}
              />
              <Area type="monotone" dataKey="kgSaved" stroke="var(--color-primary)" fill="url(#kgSavedFill)" strokeWidth={2} />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  );
}
