"use client";

import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useTenants } from "../api";

export function TenantsTable() {
  const { data, isLoading, isError } = useTenants();

  if (isLoading) return <Skeleton className="h-40 w-full" />;
  if (isError) return <p className="text-sm text-muted-foreground">Couldn&apos;t load tenants.</p>;
  if (!data || data.length === 0) return <p className="text-sm text-muted-foreground">No tenants found.</p>;

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Name</TableHead>
          <TableHead>Region</TableHead>
          <TableHead>Country</TableHead>
          <TableHead>Status</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.map((t) => (
          <TableRow key={t.id}>
            <TableCell className="font-medium">{t.name}</TableCell>
            <TableCell className="text-muted-foreground">{t.regionId ?? "—"}</TableCell>
            <TableCell className="text-muted-foreground">{t.countryCode ?? "—"}</TableCell>
            <TableCell>
              <Badge variant={t.status === "ACTIVE" ? "secondary" : "outline"}>{t.status ?? "Unknown"}</Badge>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
