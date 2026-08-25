"use client";

import { BarChart3, Leaf, Package, Recycle } from "lucide-react";
import { MetricCard } from "@/features/dashboard/components/MetricCard";
import { TrendChart } from "@/features/analytics/components/TrendChart";
import { CategoryBreakdownChart } from "@/features/analytics/components/CategoryBreakdownChart";
import { useCommunitySummary } from "@/features/analytics/api";

export default function AnalyticsPage() {
  const { data, isLoading } = useCommunitySummary();

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="flex items-center gap-2 text-xl font-semibold tracking-tight">
          <BarChart3 className="size-5 text-primary" />
          Analytics
        </h1>
        <p className="text-sm text-muted-foreground">Community-wide impact, trends, and category breakdown.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <MetricCard
          label="Total rescues"
          value={data?.rescueCount ?? 0}
          icon={Package}
          accent="primary"
          loading={isLoading}
        />
        <MetricCard
          label="Food saved"
          value={`${(data?.estimatedKgSaved ?? 0).toFixed(0)} kg`}
          icon={Leaf}
          accent="success"
          loading={isLoading}
        />
        <MetricCard
          label="CO2 avoided"
          value={`${(data?.estimatedCo2SavedKg ?? 0).toFixed(0)} kg`}
          icon={Recycle}
          accent="info"
          loading={isLoading}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <TrendChart />
        <CategoryBreakdownChart />
      </div>
    </div>
  );
}
