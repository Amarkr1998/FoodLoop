"use client";

import { useEffect, useState } from "react";

// Falls back to the seeded default tenant's region center (see
// services/tenant's V2__seed_default_tenant.sql, 'IN-DEFAULT') when the
// browser denies/lacks geolocation, so geospatial search still works.
const FALLBACK_COORDS = { lat: 12.9716, lng: 77.5946 };

export function useGeolocation() {
  const [coords, setCoords] = useState(FALLBACK_COORDS);
  const [permissionDenied, setPermissionDenied] = useState(false);

  useEffect(() => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (pos) => setCoords({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      () => setPermissionDenied(true),
      { timeout: 5000 },
    );
  }, []);

  return { coords, permissionDenied };
}
