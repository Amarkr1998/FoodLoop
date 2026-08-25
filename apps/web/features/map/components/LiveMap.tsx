"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Map, { Marker, Popup, NavigationControl, type MapRef } from "react-map-gl/maplibre";
import "maplibre-gl/dist/maplibre-gl.css";
import { Package, Truck, Building2, Timer, X } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";
import { useGeolocation } from "@/lib/use-geolocation";
import { useMapDonors, useMapPickups, useMapOrganizations } from "../api";

// CARTO's Positron basemap style — no API key required, unlike Mapbox's
// own styles (we have no Mapbox token configured in this environment).
const MAP_STYLE = "https://basemaps.cartocdn.com/gl/positron-gl-style/style.json";

type Kind = "donor" | "pickup" | "ngo" | "expiring";

interface MapEntity {
  id: string;
  kind: Kind;
  lat: number;
  lng: number;
  title: string;
  subtitle?: string;
}

const KIND_CONFIG: Record<Kind, { icon: typeof Package; className: string; label: string }> = {
  donor: { icon: Package, className: "bg-primary text-primary-foreground", label: "Donors" },
  pickup: { icon: Truck, className: "bg-warning text-warning-foreground", label: "Pickups" },
  ngo: { icon: Building2, className: "bg-info text-info-foreground", label: "NGOs" },
  expiring: { icon: Timer, className: "bg-destructive text-destructive-foreground", label: "Expiring soon" },
};

export function LiveMap() {
  const { coords } = useGeolocation();
  const [radiusKm] = useState(20);
  const [visible, setVisible] = useState<Record<Kind, boolean>>({ donor: true, pickup: true, ngo: true, expiring: true });
  const [selected, setSelected] = useState<MapEntity | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const mapRef = useRef<MapRef>(null);

  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(interval);
  }, []);

  // Geolocation resolves after the map has already mounted with the
  // fallback coordinates as its (mount-only) initialViewState — without
  // this, the map silently stays centered on the fallback region forever
  // even after the data queries below correctly refetch against the real
  // coordinates, leaving every marker rendered outside the visible viewport.
  useEffect(() => {
    mapRef.current?.flyTo({ center: [coords.lng, coords.lat], duration: 800 });
  }, [coords.lat, coords.lng]);

  const { data: donorsPage } = useMapDonors(coords.lat, coords.lng, radiusKm);
  const { data: pickupsPage } = useMapPickups(coords.lat, coords.lng, radiusKm);
  const { data: orgsPage } = useMapOrganizations(coords.lat, coords.lng, radiusKm, "NGO");

  const entities = useMemo<MapEntity[]>(() => {
    const list: MapEntity[] = [];
    for (const d of donorsPage?.content ?? []) {
      if (d.latitude === undefined || d.longitude === undefined) continue;
      const expiringSoon = d.expiryTime ? new Date(d.expiryTime).getTime() - now < 2 * 60 * 60_000 : false;
      list.push({
        id: `donor-${d.id}`,
        kind: expiringSoon ? "expiring" : "donor",
        lat: d.latitude,
        lng: d.longitude,
        title: d.title ?? "Donation",
        subtitle: d.status,
      });
    }
    for (const p of pickupsPage?.content ?? []) {
      if (p.latitude === undefined || p.longitude === undefined) continue;
      list.push({ id: `pickup-${p.id}`, kind: "pickup", lat: p.latitude, lng: p.longitude, title: "Pickup task", subtitle: p.status });
    }
    for (const o of orgsPage?.content ?? []) {
      if (o.latitude === undefined || o.longitude === undefined) continue;
      list.push({ id: `ngo-${o.id}`, kind: "ngo", lat: o.latitude, lng: o.longitude, title: o.name ?? "NGO", subtitle: o.verificationStatus });
    }
    return list;
  }, [donorsPage, pickupsPage, orgsPage, now]);

  const visibleEntities = entities.filter((e) => visible[e.kind]);

  return (
    <div className="relative flex h-full w-full">
      <Map
        ref={mapRef}
        initialViewState={{ longitude: coords.lng, latitude: coords.lat, zoom: 12 }}
        mapStyle={MAP_STYLE}
        style={{ width: "100%", height: "100%" }}
      >
        <NavigationControl position="top-right" />
        {visibleEntities.map((entity) => {
          const { icon: Icon, className } = KIND_CONFIG[entity.kind];
          return (
            <Marker
              key={entity.id}
              longitude={entity.lng}
              latitude={entity.lat}
              onClick={(e) => {
                e.originalEvent.stopPropagation();
                setSelected(entity);
              }}
            >
              <button
                className={cn(
                  "flex size-7 items-center justify-center rounded-full border-2 border-background shadow-sm transition-transform hover:scale-110",
                  className,
                )}
              >
                <Icon className="size-3.5" />
              </button>
            </Marker>
          );
        })}
        {selected && (
          <Popup longitude={selected.lng} latitude={selected.lat} onClose={() => setSelected(null)} closeButton={false} offset={16}>
            <div className="min-w-40 space-y-1">
              <p className="text-sm font-medium">{selected.title}</p>
              {selected.subtitle && <Badge variant="outline">{selected.subtitle}</Badge>}
            </div>
          </Popup>
        )}
      </Map>

      <Card className="absolute left-3 top-3 w-52 shadow-md">
        <CardContent className="space-y-2 p-3">
          <p className="text-xs font-medium text-muted-foreground">Layers</p>
          {(Object.keys(KIND_CONFIG) as Kind[]).map((kind) => {
            const { label } = KIND_CONFIG[kind];
            return (
              <label key={kind} className="flex cursor-pointer items-center gap-2 text-sm">
                <Checkbox
                  checked={visible[kind]}
                  onCheckedChange={(checked) => setVisible((v) => ({ ...v, [kind]: !!checked }))}
                />
                {label}
                <span className="ml-auto text-xs text-muted-foreground">
                  {entities.filter((e) => e.kind === kind).length}
                </span>
              </label>
            );
          })}
        </CardContent>
      </Card>

      {selected && (
        <Card className="absolute bottom-3 left-3 right-3 shadow-md sm:right-auto sm:w-80">
          <CardContent className="flex items-start justify-between gap-3 p-4">
            <div>
              <p className="font-medium">{selected.title}</p>
              {selected.subtitle && <Badge variant="outline" className="mt-1">{selected.subtitle}</Badge>}
              <p className="mt-1 text-xs text-muted-foreground">
                {selected.lat.toFixed(4)}, {selected.lng.toFixed(4)}
              </p>
            </div>
            <Button variant="ghost" size="icon" className="size-6" onClick={() => setSelected(null)}>
              <X className="size-4" />
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
