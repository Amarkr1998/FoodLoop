"use client";

import { GitMerge } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useDonations } from "@/features/donations/api";
import { DonationCard } from "@/features/donations/components/DonationCard";
import { useGeolocation } from "@/lib/use-geolocation";

export default function MatchingPage() {
  const { coords } = useGeolocation();
  const { data, isLoading } = useDonations({ lat: coords.lat, lng: coords.lng, radiusKm: 25 });
  const listings = (data?.content ?? []).filter((l) => l.status === "AVAILABLE" || l.status === "DRAFT");

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="flex items-center gap-2 text-xl font-semibold tracking-tight">
          <GitMerge className="size-5 text-primary" />
          Matching
        </h1>
        <p className="text-sm text-muted-foreground">
          Select a donation to see recommended receivers and match it.
        </p>
      </div>

      <div className="space-y-2">
        {isLoading ? (
          <Skeleton className="h-[68px] w-full" />
        ) : listings.length === 0 ? (
          <Card className="shadow-none">
            <CardContent className="py-10 text-center text-sm text-muted-foreground">
              No donations awaiting a match right now.
            </CardContent>
          </Card>
        ) : (
          listings.map((listing) => <DonationCard key={listing.id} listing={listing} />)
        )}
      </div>
    </div>
  );
}
