"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { useGeolocation } from "@/lib/use-geolocation";
import { useCreateDonation, usePublishDonation } from "../api";
import { createDonationSchema, FOOD_CATEGORIES, QUANTITY_UNITS, type CreateDonationValues } from "../schemas";

function toDatetimeLocalDefault(hoursFromNow: number) {
  const d = new Date(Date.now() + hoursFromNow * 60 * 60_000);
  d.setSeconds(0, 0);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
}

export function DonationForm({ donorOrgId }: { donorOrgId: string }) {
  const router = useRouter();
  const { coords, permissionDenied } = useGeolocation();
  const createDonation = useCreateDonation();
  const publishDonation = usePublishDonation();

  const form = useForm<CreateDonationValues>({
    resolver: zodResolver(createDonationSchema),
    defaultValues: {
      title: "",
      description: "",
      foodCategory: "COOKED_MEAL",
      quantityValue: 1,
      quantityUnit: "SERVINGS",
      expiryTime: toDatetimeLocalDefault(6),
      pickupStartTime: toDatetimeLocalDefault(1),
      pickupEndTime: toDatetimeLocalDefault(4),
      latitude: coords.lat,
      longitude: coords.lng,
    },
  });

  // Geolocation resolves asynchronously after this form has already
  // mounted with the fallback coordinates as its defaultValues (RHF
  // doesn't re-read defaultValues reactively) — without this, every
  // donation would silently save at the fallback location regardless of
  // where the donor actually is, since lat/lng aren't user-editable fields.
  useEffect(() => {
    form.setValue("latitude", coords.lat);
    form.setValue("longitude", coords.lng);
  }, [coords.lat, coords.lng, form]);

  async function onSubmit(values: CreateDonationValues, publish: boolean) {
    const created = await createDonation.mutateAsync({
      donorOrgId,
      title: values.title,
      description: values.description,
      foodCategory: values.foodCategory,
      quantityValue: values.quantityValue,
      quantityUnit: values.quantityUnit,
      estimatedServings: values.estimatedServings,
      expiryTime: new Date(values.expiryTime).toISOString(),
      pickupStartTime: new Date(values.pickupStartTime).toISOString(),
      pickupEndTime: new Date(values.pickupEndTime).toISOString(),
      latitude: values.latitude,
      longitude: values.longitude,
    });
    if (publish && created.id) {
      await publishDonation.mutateAsync(created.id);
    }
    router.push(`/donations/${created.id}`);
  }

  const pending = createDonation.isPending || publishDonation.isPending;

  return (
    <Form {...form}>
      <form className="space-y-5">
        <FormField
          control={form.control}
          name="title"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Title</FormLabel>
              <FormControl>
                <Input placeholder="e.g. 20 vegetable curry servings" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Description</FormLabel>
              <FormControl>
                <Textarea rows={3} {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="foodCategory"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Category</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {FOOD_CATEGORIES.map((c) => (
                      <SelectItem key={c} value={c}>
                        {c.replace(/_/g, " ")}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="estimatedServings"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Estimated servings</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={1}
                    {...field}
                    value={field.value ?? ""}
                    onChange={(e) => field.onChange(e.target.value === "" ? undefined : Number(e.target.value))}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="quantityValue"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Quantity</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    step="0.1"
                    min={0}
                    {...field}
                    onChange={(e) => field.onChange(Number(e.target.value))}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="quantityUnit"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Unit</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {QUANTITY_UNITS.map((u) => (
                      <SelectItem key={u} value={u}>
                        {u}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <FormField
          control={form.control}
          name="expiryTime"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Expires at</FormLabel>
              <FormControl>
                <Input type="datetime-local" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="pickupStartTime"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Pickup window start</FormLabel>
                <FormControl>
                  <Input type="datetime-local" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="pickupEndTime"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Pickup window end</FormLabel>
                <FormControl>
                  <Input type="datetime-local" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <MapPin className="size-3.5" />
          {permissionDenied
            ? "Location access denied — using the default pickup region instead."
            : `Pickup location set to your current position (${coords.lat.toFixed(4)}, ${coords.lng.toFixed(4)}).`}
        </p>

        <div className="flex gap-3 pt-2">
          <Button
            type="button"
            variant="outline"
            disabled={pending}
            onClick={form.handleSubmit((v) => onSubmit(v, false))}
          >
            Save as draft
          </Button>
          <Button type="button" disabled={pending} onClick={form.handleSubmit((v) => onSubmit(v, true))}>
            {pending ? "Saving..." : "Save & publish"}
          </Button>
        </div>
      </form>
    </Form>
  );
}
