"use client";

import { ShieldCheck } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { TenantsTable } from "@/features/admin/components/TenantsTable";
import { OrganizationsPanel } from "@/features/admin/components/OrganizationsPanel";
import { UsersPanel } from "@/features/admin/components/UsersPanel";
import { RolesPanel } from "@/features/admin/components/RolesPanel";
import { AuditAndHealthPanel } from "@/features/admin/components/AuditAndHealthPanel";

export default function AdminPage() {
  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="flex items-center gap-2 text-xl font-semibold tracking-tight">
          <ShieldCheck className="size-5 text-primary" />
          Admin
        </h1>
        <p className="text-sm text-muted-foreground">Tenants, organizations, users, roles, and platform health.</p>
      </div>

      <Tabs defaultValue="tenants">
        <TabsList>
          <TabsTrigger value="tenants">Tenants</TabsTrigger>
          <TabsTrigger value="organizations">Organizations</TabsTrigger>
          <TabsTrigger value="users">
            Users <Badge variant="outline" className="ml-1.5 text-[10px] text-muted-foreground">Preview</Badge>
          </TabsTrigger>
          <TabsTrigger value="roles">
            Roles <Badge variant="outline" className="ml-1.5 text-[10px] text-muted-foreground">Preview</Badge>
          </TabsTrigger>
          <TabsTrigger value="audit">
            Audit &amp; Health <Badge variant="outline" className="ml-1.5 text-[10px] text-muted-foreground">Preview</Badge>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="tenants" className="mt-4">
          <TenantsTable />
        </TabsContent>
        <TabsContent value="organizations" className="mt-4">
          <OrganizationsPanel />
        </TabsContent>
        <TabsContent value="users" className="mt-4">
          <UsersPanel />
        </TabsContent>
        <TabsContent value="roles" className="mt-4">
          <RolesPanel />
        </TabsContent>
        <TabsContent value="audit" className="mt-4">
          <AuditAndHealthPanel />
        </TabsContent>
      </Tabs>
    </div>
  );
}
