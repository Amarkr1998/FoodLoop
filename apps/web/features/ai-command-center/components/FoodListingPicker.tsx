"use client";

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useDonations } from "@/features/donations/api";
import { useGeolocation } from "@/lib/use-geolocation";

export function FoodListingPicker({ value, onChange }: { value: string; onChange: (id: string) => void }) {
  const { coords } = useGeolocation();
  const { data, isLoading } = useDonations({ lat: coords.lat, lng: coords.lng, radiusKm: 25, size: 50 });
  const listings = data?.content ?? [];

  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger>
        <SelectValue placeholder={isLoading ? "Loading donations…" : "Select a food listing"} />
      </SelectTrigger>
      <SelectContent>
        {listings.length === 0 && !isLoading ? (
          <div className="px-2 py-1.5 text-sm text-muted-foreground">No nearby listings found.</div>
        ) : (
          listings
            .filter((l) => !!l.id)
            .map((l) => (
              <SelectItem key={l.id} value={l.id as string}>
                {l.title ?? l.id}
              </SelectItem>
            ))
        )}
      </SelectContent>
    </Select>
  );
}
