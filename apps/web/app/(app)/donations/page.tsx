"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Plus, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useDonations } from "@/features/donations/api";
import { DonationCard } from "@/features/donations/components/DonationCard";
import { useGeolocation } from "@/lib/use-geolocation";

const CATEGORIES = ["COOKED_MEAL", "PACKAGED", "PRODUCE", "BAKERY", "DAIRY", "BEVERAGE", "OTHER"];

export default function DonationsPage() {
  const { coords } = useGeolocation();
  const [radiusKm, setRadiusKm] = useState(10);
  const [category, setCategory] = useState<string>("all");
  const [quickFilter, setQuickFilter] = useState("");

  const { data, isLoading, isError } = useDonations({
    lat: coords.lat,
    lng: coords.lng,
    radiusKm,
    category: category === "all" ? undefined : category,
  });

  const filtered = useMemo(() => {
    const content = data?.content ?? [];
    if (!quickFilter.trim()) return content;
    const q = quickFilter.toLowerCase();
    return content.filter((l) => l.title?.toLowerCase().includes(q));
  }, [data, quickFilter]);

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Donations</h1>
          <p className="text-sm text-muted-foreground">Surplus food listings near you.</p>
        </div>
        <Link href="/donations/new">
          <Button>
            <Plus className="size-4" />
            New donation
          </Button>
        </Link>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative w-full max-w-xs">
          <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Filter by title..."
            className="pl-8"
            value={quickFilter}
            onChange={(e) => setQuickFilter(e.target.value)}
          />
        </div>
        <Select value={category} onValueChange={setCategory}>
          <SelectTrigger className="w-44">
            <SelectValue placeholder="Category" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All categories</SelectItem>
            {CATEGORIES.map((c) => (
              <SelectItem key={c} value={c}>
                {c.replace(/_/g, " ")}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={String(radiusKm)} onValueChange={(v) => setRadiusKm(Number(v))}>
          <SelectTrigger className="w-32">
            <SelectValue placeholder="Radius" />
          </SelectTrigger>
          <SelectContent>
            {[5, 10, 25, 50].map((r) => (
              <SelectItem key={r} value={String(r)}>
                {r} km
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        {isLoading ? (
          Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-[68px] w-full" />)
        ) : isError ? (
          <p className="py-12 text-center text-sm text-muted-foreground">Couldn&apos;t load donations. Try again shortly.</p>
        ) : filtered.length === 0 ? (
          <p className="py-12 text-center text-sm text-muted-foreground">No donations match your filters.</p>
        ) : (
          filtered.map((listing) => <DonationCard key={listing.id} listing={listing} />)
        )}
      </div>
    </div>
  );
}
