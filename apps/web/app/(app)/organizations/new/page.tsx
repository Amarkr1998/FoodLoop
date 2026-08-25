"use client";

import { useActionState, useState } from "react";
import { Building2 } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { createOrganization, type CreateOrgState } from "./actions";

const initialState: CreateOrgState = {};

const ORG_TYPES = [
  "DONOR_RESTAURANT",
  "DONOR_HOTEL",
  "DONOR_GROCERY",
  "DONOR_CATERER",
  "DONOR_HOME_COOK",
  "NGO",
  "FOOD_BANK",
  "CORPORATE",
  "INDIVIDUAL",
];

export default function NewOrganizationPage() {
  const [state, formAction, pending] = useActionState(createOrganization, initialState);
  const [type, setType] = useState("DONOR_RESTAURANT");

  return (
    <div className="mx-auto max-w-md p-6">
      <Card className="shadow-none">
        <CardHeader>
          <div className="mb-1 flex size-10 items-center justify-center rounded-lg bg-primary/10">
            <Building2 className="size-5 text-primary" />
          </div>
          <CardTitle>Register your organization</CardTitle>
          <CardDescription>Set up your organization to start donating or receiving food.</CardDescription>
        </CardHeader>
        <CardContent>
          <form action={formAction} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="name">Organization name</Label>
              <Input id="name" name="name" required />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="type">Type</Label>
              <Select value={type} onValueChange={setType}>
                <SelectTrigger id="type" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ORG_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t.replace(/_/g, " ")}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <input type="hidden" name="type" value={type} />
            </div>
            {state.error && <p className="text-sm text-destructive">{state.error}</p>}
            <Button type="submit" disabled={pending} className="w-full">
              {pending ? "Creating..." : "Create organization"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
