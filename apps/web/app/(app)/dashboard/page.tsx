"use client";

import { Sprout, Cloud, GitMerge, Truck, Package, HeartHandshake } from "lucide-react";
import { MetricCard } from "@/features/dashboard/components/MetricCard";
import { ImpactTrendChart } from "@/features/dashboard/components/ImpactTrendChart";
import { LiveActivityFeed } from "@/features/dashboard/components/LiveActivityFeed";
import { OperationalAlerts } from "@/features/dashboard/components/OperationalAlerts";
import { ExpiringFoodPanel } from "@/features/dashboard/components/ExpiringFoodPanel";
import { useCommunityImpact } from "@/features/dashboard/api";
import { mockQuickStats } from "@/features/dashboard/mock-data";

export default function DashboardPage() {
  const { data: impact, isLoading: impactLoading } = useCommunityImpact();

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Dashboard</h1>
        <p className="text-sm text-muted-foreground">Community-wide impact and live operations at a glance.</p>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <MetricCard label="Food rescued" value={impact?.rescueCount ?? 0} icon={Sprout} accent="success" loading={impactLoading} hint="All-time completed rescues" />
        <MetricCard
          label="CO₂ saved"
          value={`${(impact?.estimatedCo2SavedKg ?? 0).toFixed(0)} kg`}
          icon={Cloud}
          accent="info"
          loading={impactLoading}
        />
        <MetricCard label="Active matches" value={mockQuickStats.activeMatches} icon={GitMerge} accent="primary" hint="Preview data" />
        <MetricCard label="Pending pickups" value={mockQuickStats.pendingPickups} icon={Truck} accent="warning" hint="Preview data" />
        <MetricCard label="Active donations" value={mockQuickStats.activeDonations} icon={Package} accent="primary" hint="Preview data" />
        <MetricCard label="NGO partners" value={mockQuickStats.ngoPartners} icon={HeartHandshake} accent="info" hint="Preview data" />
        <MetricCard
          label="Kg saved"
          value={`${(impact?.estimatedKgSaved ?? 0).toFixed(0)} kg`}
          icon={Sprout}
          accent="success"
          loading={impactLoading}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <ImpactTrendChart />
        </div>
        <ExpiringFoodPanel />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <LiveActivityFeed />
        <OperationalAlerts />
      </div>
    </div>
  );
}
