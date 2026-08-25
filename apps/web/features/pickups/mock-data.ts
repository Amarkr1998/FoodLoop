/**
 * No aggregate "list pickups by status" endpoint exists yet — pickup.yaml
 * only has GET /pickups/available (geospatial, unassigned only) and
 * GET /pickups/delayed. Assigned/en-route/completed can only be looked up
 * per-id today, not queried as a set. Preview cards here stand in for
 * those columns until such an endpoint exists.
 */
export interface PreviewPickupCard {
  id: string;
  volunteerName: string;
  eta: string;
}

export const mockAssigned: PreviewPickupCard[] = [
  { id: "a1", volunteerName: "Rahul M.", eta: "18 min" },
  { id: "a2", volunteerName: "Priya S.", eta: "32 min" },
];

export const mockCompleted: PreviewPickupCard[] = [
  { id: "c1", volunteerName: "Arjun K.", eta: "Completed 12 min ago" },
  { id: "c2", volunteerName: "Deepa R.", eta: "Completed 41 min ago" },
  { id: "c3", volunteerName: "Sanjay P.", eta: "Completed 1h ago" },
];
