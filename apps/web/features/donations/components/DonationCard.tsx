import Link from "next/link";
import { Package } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { DonationStatusBadge } from "./DonationStatusBadge";
import { ExpiryCountdown } from "./ExpiryCountdown";
import type { FoodListing } from "../api";

export function DonationCard({ listing }: { listing: FoodListing }) {
  return (
    <Link href={`/donations/${listing.id}`}>
      <Card className="shadow-none transition-colors hover:border-primary/40">
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
        </CardContent>
      </Card>
    </Link>
  );
}
