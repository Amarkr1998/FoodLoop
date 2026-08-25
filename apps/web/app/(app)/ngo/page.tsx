"use client";

import { HeartHandshake, Package, History } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Progress } from "@/components/ui/progress";
import { DonationStatusBadge } from "@/features/donations/components/DonationStatusBadge";
import { ExpiryCountdown } from "@/features/donations/components/ExpiryCountdown";
import { useDonations } from "@/features/donations/api";
import { ClaimButton } from "@/features/ngo/components/ClaimButton";
import { mockCapacity, mockDemand, mockPickupHistory } from "@/features/ngo/mock-data";
import { useGeolocation } from "@/lib/use-geolocation";

export default function NgoPortalPage() {
  const { coords } = useGeolocation();
  const { data, isLoading } = useDonations({ lat: coords.lat, lng: coords.lng, radiusKm: 15 });
  const available = (data?.content ?? []).filter((l) => l.status === "AVAILABLE");

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="flex items-center gap-2 text-xl font-semibold tracking-tight">
          <HeartHandshake className="size-5 text-primary" />
          NGO Portal
        </h1>
        <p className="text-sm text-muted-foreground">Nearby available donations you can claim.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="space-y-3 lg:col-span-2">
          {isLoading ? (
            <Skeleton className="h-20 w-full" />
          ) : available.length === 0 ? (
            <Card className="shadow-none">
              <CardContent className="py-10 text-center text-sm text-muted-foreground">
                No available donations nearby right now.
              </CardContent>
            </Card>
          ) : (
            available.map((listing) => (
              <Card key={listing.id} className="shadow-none">
                <CardContent className="flex items-center gap-4 px-5 py-4">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted">
                    <Package className="size-4.5 text-muted-foreground" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <p className="truncate font-medium">{listing.title}</p>
                      {listing.status && <DonationStatusBadge status={listing.status} />}
                    </div>
                    <p className="mt-0.5 text-sm text-muted-foreground">
                      {listing.quantityValue} {listing.quantityUnit} · {listing.foodCategory?.replace(/_/g, " ")}
                    </p>
                  </div>
                  {listing.expiryTime && <ExpiryCountdown expiryTime={listing.expiryTime} />}
                  {listing.id && <ClaimButton foodListingId={listing.id} title={listing.title ?? "this donation"} />}
                </CardContent>
              </Card>
            ))
          )}
        </div>

        <div className="space-y-4">
          <Card className="shadow-none">
            <CardHeader className="flex-row items-center justify-between space-y-0">
              <CardTitle className="text-base">Capacity</CardTitle>
              <Badge variant="outline" className="text-[10px] text-muted-foreground">Preview</Badge>
            </CardHeader>
            <CardContent>
              <Progress value={(mockCapacity.current / mockCapacity.max) * 100} />
              <p className="mt-2 text-sm text-muted-foreground">
                {mockCapacity.current} / {mockCapacity.max} servings capacity used today
              </p>
            </CardContent>
          </Card>

          <Card className="shadow-none">
            <CardHeader className="flex-row items-center justify-between space-y-0">
              <CardTitle className="text-base">Demand</CardTitle>
              <Badge variant="outline" className="text-[10px] text-muted-foreground">Preview</Badge>
            </CardHeader>
            <CardContent className="space-y-2">
              {mockDemand.map((d) => (
                <div key={d.category} className="flex items-center justify-between text-sm">
                  <span>{d.category.replace(/_/g, " ")}</span>
                  <Badge variant="secondary">{d.demand}</Badge>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="shadow-none">
            <CardHeader className="flex-row items-center justify-between space-y-0">
              <CardTitle className="flex items-center gap-2 text-base">
                <History className="size-4" />
                Pickup history
              </CardTitle>
              <Badge variant="outline" className="text-[10px] text-muted-foreground">Preview</Badge>
            </CardHeader>
            <CardContent className="space-y-2">
              {mockPickupHistory.map((h) => (
                <div key={h.id} className="text-sm">
                  <p className="font-medium">{h.title}</p>
                  <p className="text-xs text-muted-foreground">{h.completedAt}</p>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
