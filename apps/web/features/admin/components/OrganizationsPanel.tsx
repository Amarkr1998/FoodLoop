"use client";

import { useState } from "react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { useGeolocation } from "@/lib/use-geolocation";
import { useAddOrgMember, useOrganizationsNearby, useOrgMembers } from "../api";

export function OrganizationsPanel() {
  const { coords } = useGeolocation();
  const { data, isLoading, isError } = useOrganizationsNearby(coords.lat, coords.lng);
  const [selectedOrgId, setSelectedOrgId] = useState<string | null>(null);

  const orgs = (data?.content ?? []).filter((o) => !!o.id);

  return (
    <>
      {isLoading ? (
        <Skeleton className="h-40 w-full" />
      ) : isError ? (
        <p className="text-sm text-muted-foreground">Couldn&apos;t load organizations.</p>
      ) : orgs.length === 0 ? (
        <p className="text-sm text-muted-foreground">No organizations found nearby.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Verification</TableHead>
              <TableHead className="text-right">Members</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {orgs.map((o) => (
              <TableRow key={o.id}>
                <TableCell className="font-medium">{o.name}</TableCell>
                <TableCell className="text-muted-foreground">{o.type?.replace(/_/g, " ")}</TableCell>
                <TableCell>
                  <Badge variant={o.verificationStatus === "VERIFIED" ? "secondary" : "outline"}>
                    {o.verificationStatus ?? "Unknown"}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button variant="ghost" size="sm" onClick={() => setSelectedOrgId(o.id ?? null)}>
                    View
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Sheet open={!!selectedOrgId} onOpenChange={(open) => !open && setSelectedOrgId(null)}>
        <SheetContent>
          <SheetHeader>
            <SheetTitle>Organization members</SheetTitle>
          </SheetHeader>
          {selectedOrgId && <OrgMembersList orgId={selectedOrgId} />}
        </SheetContent>
      </Sheet>
    </>
  );
}

function OrgMembersList({ orgId }: { orgId: string }) {
  const { data, isLoading } = useOrgMembers(orgId);
  const [userId, setUserId] = useState("");
  const [role, setRole] = useState<"ORG_ADMIN" | "MEMBER">("MEMBER");
  const addMember = useAddOrgMember();

  return (
    <div className="space-y-4 px-4">
      {isLoading ? (
        <Skeleton className="h-20" />
      ) : !data || data.length === 0 ? (
        <p className="text-sm text-muted-foreground">No members yet.</p>
      ) : (
        <div className="space-y-2">
          {data.map((m) => (
            <div key={m.userId} className="flex items-center justify-between text-sm">
              <span className="truncate text-muted-foreground">{m.userId}</span>
              <Badge variant="outline">{m.role}</Badge>
            </div>
          ))}
        </div>
      )}

      <div className="space-y-2 border-t border-border pt-4">
        <p className="text-xs font-medium text-muted-foreground">Add member</p>
        <Input placeholder="User ID (UUID)" value={userId} onChange={(e) => setUserId(e.target.value)} />
        <div className="flex gap-2">
          <Select value={role} onValueChange={(v) => setRole(v as "ORG_ADMIN" | "MEMBER")}>
            <SelectTrigger className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="MEMBER">Member</SelectItem>
              <SelectItem value="ORG_ADMIN">Org admin</SelectItem>
            </SelectContent>
          </Select>
          <Button
            disabled={!userId || addMember.isPending}
            onClick={() => {
              addMember.mutate(
                { orgId, userId, role },
                { onSuccess: () => setUserId("") },
              );
            }}
          >
            Add
          </Button>
        </div>
      </div>
    </div>
  );
}
