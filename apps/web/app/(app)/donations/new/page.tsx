"use client";

import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useOrgContextStore } from "@/lib/stores/org-context-store";
import { DonationForm } from "@/features/donations/components/DonationForm";

export default function NewDonationPage() {
  const activeOrgId = useOrgContextStore((s) => s.activeOrgId);

  if (!activeOrgId) {
    return (
      <div className="p-6">
        <Card className="mx-auto max-w-md shadow-none">
          <CardHeader>
            <CardTitle className="text-base">No organization selected</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">
              You need to register or select an organization before creating a donation.
            </p>
            <Link href="/organizations/new">
              <Button>Register your organization</Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6 p-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">List surplus food</h1>
        <p className="text-sm text-muted-foreground">Publish a donation for nearby receivers to discover.</p>
      </div>
      <DonationForm donorOrgId={activeOrgId} />
    </div>
  );
}
