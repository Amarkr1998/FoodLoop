import type { LucideIcon } from "lucide-react";
import {
  LayoutDashboard,
  Package,
  GitMerge,
  Truck,
  MapPin,
  Timer,
  HeartHandshake,
  Bot,
  BarChart3,
  ShieldCheck,
} from "lucide-react";
import type { FoodLoopRealmRole } from "@foodloop/auth";

export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
  /** UX-only gating — every route/action is independently re-checked server-side (never rely on this for real authorization). */
  roles?: FoodLoopRealmRole[];
}

export const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/donations", label: "Donations", icon: Package, roles: ["DONOR", "ORG_ADMIN", "ADMIN"] },
  { href: "/matching", label: "Matching", icon: GitMerge, roles: ["DONOR", "ORG_ADMIN", "NGO_ADMIN", "ADMIN"] },
  { href: "/pickups", label: "Pickup Operations", icon: Truck, roles: ["VOLUNTEER", "ORG_ADMIN", "ADMIN"] },
  { href: "/map", label: "Live Map", icon: MapPin },
  { href: "/rescue", label: "Expiry Rescue", icon: Timer, roles: ["ORG_ADMIN", "TRUST_OPS", "ADMIN"] },
  { href: "/ngo", label: "NGO Portal", icon: HeartHandshake, roles: ["NGO_ADMIN", "NGO_OPS", "RECEIVER", "ADMIN"] },
  { href: "/ai", label: "AI Command Center", icon: Bot, roles: ["TRUST_OPS", "ADMIN"] },
  { href: "/analytics", label: "Analytics", icon: BarChart3, roles: ["ORG_ADMIN", "ADMIN"] },
  { href: "/admin", label: "Admin", icon: ShieldCheck, roles: ["ADMIN"] },
];

export function visibleNavItems(roles: string[]): NavItem[] {
  return NAV_ITEMS.filter((item) => !item.roles || item.roles.some((r) => roles.includes(r)));
}
