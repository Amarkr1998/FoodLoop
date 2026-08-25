"use client";

import { use } from "react";
import { ArrowLeft, GitMerge } from "lucide-react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { DonationStatusBadge } from "@/features/donations/components/DonationStatusBadge";
import { ExpiryCountdown } from "@/features/donations/components/ExpiryCountdown";
import { useDonation, useDonationMatches, usePublishDonation, useCancelDonation } from "@/features/donations/api";
import { MatchCandidatesPanel } from "@/features/matching/components/MatchCandidatesPanel";

export default function DonationDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { data: listing, isLoading } = useDonation(id);
  const { data: matches } = useDonationMatches(id);
  const publish = usePublishDonation();
  const cancel = useCancelDonation();

  if (isLoading || !listing) {
    return (
      <div className="space-y-4 p-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-6">
      <Link href="/donations" className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" />
        Back to donations
      </Link>

      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-semibold tracking-tight">{listing.title}</h1>
            {listing.status && <DonationStatusBadge status={listing.status} />}
          </div>
          <p className="mt-1 text-sm text-muted-foreground">{listing.description}</p>
        </div>
        {listing.expiryTime && <ExpiryCountdown expiryTime={listing.expiryTime} />}
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <DetailStat label="Quantity" value={`${listing.quantityValue} ${listing.quantityUnit}`} />
        <DetailStat label="Category" value={listing.foodCategory?.replace(/_/g, " ") ?? "—"} />
        <DetailStat label="Servings" value={listing.estimatedServings ? String(listing.estimatedServings) : "—"} />
        <DetailStat
          label="Pickup window"
          value={
            listing.pickupStartTime && listing.pickupEndTime
              ? `${new Date(listing.pickupStartTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })} – ${new Date(listing.pickupEndTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`
              : "—"
          }
        />
      </div>

      <Card className="shadow-none">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <GitMerge className="size-4" />
            Matches
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {!matches || matches.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">No match proposals yet.</p>
          ) : (
            matches.map((m) => (
              <div key={m.id} className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm">
                <div>
                  <p className="font-medium">Receiver {m.receiverOrgId?.slice(0, 8)}</p>
                  {m.aiRationale && <p className="text-xs text-muted-foreground">{m.aiRationale}</p>}
                </div>
                <div className="flex items-center gap-2">
                  {typeof m.score === "number" && <Badge variant="outline">{Math.round(m.score * 100)}% match</Badge>}
                  {m.status && <Badge variant="secondary">{m.status}</Badge>}
                </div>
              </div>
            ))
          )}
        </CardContent>
      </Card>

      {(listing.status === "AVAILABLE" || listing.status === "DRAFT") && listing.id && (
        <MatchCandidatesPanel foodListingId={listing.id} />
      )}

      <div className="flex gap-3">
        {listing.status === "DRAFT" && (
          <Button disabled={publish.isPending} onClick={() => listing.id && publish.mutate(listing.id)}>
            {publish.isPending ? "Publishing..." : "Publish"}
          </Button>
        )}
        {(listing.status === "DRAFT" || listing.status === "AVAILABLE") && (
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="outline" className="text-destructive hover:text-destructive">
                Cancel donation
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Cancel this donation?</AlertDialogTitle>
                <AlertDialogDescription>
                  This removes the listing from receiver search. This can&apos;t be undone.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Keep listing</AlertDialogCancel>
                <AlertDialogAction onClick={() => listing.id && cancel.mutate(listing.id)}>
                  Cancel donation
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        )}
      </div>
    </div>
  );
}

function DetailStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-border p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-0.5 text-sm font-medium">{value}</p>
    </div>
  );
}
