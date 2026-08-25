"use client";

import { useSession, signOut } from "next-auth/react";
import { Bell, Search, ChevronDown, LogOut, User as UserIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useUiStore } from "@/lib/stores/ui-store";
import { buildKeycloakLogoutUrl } from "@/lib/keycloak-logout";
import { useMyOrganizations } from "../api";
import { MobileNav } from "./MobileNav";

function initials(name?: string | null) {
  if (!name) return "?";
  return name
    .split(" ")
    .map((p) => p[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export function Header() {
  const { data: session } = useSession();
  const { data: orgs } = useMyOrganizations();
  const setCommandPaletteOpen = useUiStore((s) => s.setCommandPaletteOpen);
  const activeOrg = orgs?.[0];

  async function handleSignOut() {
    const logoutUrl = buildKeycloakLogoutUrl(session?.idToken);
    await signOut({ redirect: false });
    window.location.href = logoutUrl;
  }

  return (
    <header className="flex h-14 shrink-0 items-center justify-between gap-4 border-b border-border bg-card px-4">
      <div className="flex items-center gap-2">
        <MobileNav />
      </div>
      <button
        onClick={() => setCommandPaletteOpen(true)}
        className="flex w-full max-w-sm items-center gap-2 rounded-md border border-input bg-background px-3 py-1.5 text-sm text-muted-foreground hover:bg-muted"
      >
        <Search className="size-4" />
        <span>Search or jump to...</span>
        <kbd className="ml-auto rounded border border-border bg-muted px-1.5 font-mono text-[10px]">⌘K</kbd>
      </button>

      <div className="flex items-center gap-3">
        {activeOrg && (
          <div className="hidden items-center gap-2 rounded-md border border-border px-3 py-1.5 text-sm sm:flex">
            <span className="font-medium">{activeOrg.name}</span>
            <ChevronDown className="size-3.5 text-muted-foreground" />
          </div>
        )}

        <Button variant="ghost" size="icon" aria-label="Notifications">
          <Bell className="size-4" />
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="gap-2 px-2">
              <Avatar className="size-7">
                <AvatarFallback className="text-xs">{initials(session?.user?.name)}</AvatarFallback>
              </Avatar>
              <span className="hidden text-sm font-medium md:inline">{session?.user?.name}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel>
              <div className="flex flex-col">
                <span className="font-medium">{session?.user?.name}</span>
                <span className="text-xs font-normal text-muted-foreground">{session?.user?.email}</span>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem>
              <UserIcon className="size-4" />
              Profile
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem variant="destructive" onClick={handleSignOut}>
              <LogOut className="size-4" />
              Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
