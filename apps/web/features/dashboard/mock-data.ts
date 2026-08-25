/**
 * Typed placeholder data for dashboard panels with no backing platform-wide
 * aggregate endpoint yet — per the brief, these are never disguised as
 * real network calls. Each corresponds to a genuine gap:
 *  - Active matches: GET /api/v1/matches requires a foodListingId (it's
 *    per-listing, not a platform-wide feed) — packages/shared-contracts/openapi/matching.yaml.
 *  - Pending pickups: GET /api/v1/pickups/available requires lat/lng (a
 *    geospatial search, not a platform-wide count) — pickup.yaml.
 *  - NGO stats, live activity, operational alerts: no event-stream/SSE/
 *    WebSocket backend exists in any service yet (confirmed against every
 *    generated spec in packages/shared-contracts/openapi/).
 * Swap these for real TanStack Query hooks the moment those endpoints
 * exist — every consumer of this module is a component, not a network call.
 */

export interface ActivityEvent {
  id: string;
  kind: "donation_created" | "match_created" | "pickup_completed" | "donation_expiring" | "ai_action";
  message: string;
  timestamp: string;
}

export const mockActivityFeed: ActivityEvent[] = [
  { id: "1", kind: "pickup_completed", message: "Pickup completed for Green Valley Bakery listing", timestamp: "2 min ago" },
  { id: "2", kind: "match_created", message: "Matched 18 servings to Hope Community Kitchen", timestamp: "6 min ago" },
  { id: "3", kind: "donation_created", message: "New donation posted: Riverside Grocers", timestamp: "14 min ago" },
  { id: "4", kind: "ai_action", message: "Matching Agent proposed 3 candidate receivers", timestamp: "21 min ago" },
  { id: "5", kind: "donation_expiring", message: "Listing expiring within 2 hours — no match yet", timestamp: "28 min ago" },
];

export interface OperationalAlert {
  id: string;
  severity: "critical" | "warning" | "info";
  message: string;
}

export const mockOperationalAlerts: OperationalAlert[] = [
  { id: "1", severity: "critical", message: "3 listings expiring within 30 minutes with no assigned pickup" },
  { id: "2", severity: "warning", message: "Volunteer capacity below demand in North district" },
  { id: "3", severity: "info", message: "2 NGO verification requests awaiting review" },
];

export const mockQuickStats = {
  activeMatches: 12,
  pendingPickups: 7,
  activeDonations: 24,
  ngoPartners: 9,
};
