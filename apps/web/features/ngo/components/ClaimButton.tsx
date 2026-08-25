"use client";

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
import { Button } from "@/components/ui/button";
import { useClaimDonation } from "@/features/donations/api";
import { useOrgContextStore } from "@/lib/stores/org-context-store";

export function ClaimButton({ foodListingId, title }: { foodListingId: string; title: string }) {
  const activeOrgId = useOrgContextStore((s) => s.activeOrgId);
  const claim = useClaimDonation();

  if (!activeOrgId) {
    return (
      <Button size="sm" variant="outline" disabled>
        Register an org to claim
      </Button>
    );
  }

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button size="sm">Claim</Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Claim &ldquo;{title}&rdquo;?</AlertDialogTitle>
          <AlertDialogDescription>
            This reserves the donation for your organization. The donor will be notified.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
          <AlertDialogAction onClick={() => claim.mutate({ id: foodListingId, receiverOrgId: activeOrgId })}>
            Confirm claim
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
