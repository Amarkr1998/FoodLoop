import Link from "next/link";
import { Building2, CheckCircle2 } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { getApiClient } from "@/lib/api";
import { SetActiveOrg } from "./SetActiveOrg";

interface OrganizationResponse {
  id: string;
  name: string;
  type: string;
  verificationStatus: string;
  createdAt: string;
}

export default async function OrganizationDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const client = await getApiClient();
  const org = await client.get<OrganizationResponse>(`/api/v1/organizations/${id}`);

  return (
    <div className="mx-auto max-w-lg space-y-6 p-6">
      <SetActiveOrg orgId={org.id} />
      <Card className="shadow-none">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
              <Building2 className="size-5 text-primary" />
            </div>
            <div>
              <CardTitle>{org.name}</CardTitle>
              <p className="text-sm text-muted-foreground">{org.type.replace(/_/g, " ")}</p>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-2">
            <Badge variant={org.verificationStatus === "VERIFIED" ? "default" : "outline"}>
              {org.verificationStatus === "VERIFIED" && <CheckCircle2 className="size-3" />}
              {org.verificationStatus}
            </Badge>
            <span className="text-xs text-muted-foreground">Created {new Date(org.createdAt).toLocaleDateString()}</span>
          </div>
          <Link href="/donations/new">
            <Button className="w-full">List surplus food</Button>
          </Link>
        </CardContent>
      </Card>
    </div>
  );
}
