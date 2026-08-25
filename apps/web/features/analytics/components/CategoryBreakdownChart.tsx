"use client";

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip, Legend } from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useCommunityBreakdown } from "../api";

const COLORS = ["var(--color-chart-1)", "var(--color-chart-2)", "var(--color-chart-3)", "var(--color-chart-4)", "var(--color-chart-5)", "#94a3b8", "#64748b"];

export function CategoryBreakdownChart() {
  const { data, isLoading, isError } = useCommunityBreakdown();
  const rows = (data ?? []).map((c) => ({
    name: c.foodCategory ? c.foodCategory.replace(/_/g, " ") : "Other",
    value: c.estimatedKgSaved ?? 0,
  }));

  return (
    <Card className="shadow-none">
      <CardHeader>
        <CardTitle className="text-base">Rescued food by category</CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-64 w-full" />
        ) : isError || rows.length === 0 ? (
          <p className="flex h-64 items-center justify-center text-sm text-muted-foreground">No category data yet.</p>
        ) : (
          <ResponsiveContainer width="100%" height={256}>
            <PieChart>
              <Pie data={rows} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={2}>
                {rows.map((_, i) => (
                  <Cell key={i} fill={COLORS[i % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  fontSize: 12,
                }}
                formatter={(value) => [`${Number(value ?? 0).toFixed(1)} kg`, "Saved"]}
              />
              <Legend wrapperStyle={{ fontSize: 12 }} />
            </PieChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  );
}
