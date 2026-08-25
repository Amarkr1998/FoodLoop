"use server";

import { redirect } from "next/navigation";
import { ApiError } from "@foodloop/api-client";
import { getApiClient } from "@/lib/api";

export interface CreateListingState {
  error?: string;
}

export async function createFoodListing(
  donorOrgId: string,
  _prev: CreateListingState,
  formData: FormData,
): Promise<CreateListingState> {
  const client = await getApiClient();

  const quantityValue = Number(formData.get("quantityValue"));
  const estimatedServings = formData.get("estimatedServings")
    ? Number(formData.get("estimatedServings"))
    : undefined;
  const pickupStartTime = new Date(String(formData.get("pickupStartTime"))).toISOString();
  const pickupEndTime = new Date(String(formData.get("pickupEndTime"))).toISOString();
  const expiryTime = new Date(String(formData.get("expiryTime"))).toISOString();
  const latitude = Number(formData.get("latitude"));
  const longitude = Number(formData.get("longitude"));

  try {
    const listing = await client.post<{ id: string }>("/api/v1/food-listings", {
      donorOrgId,
      title: String(formData.get("title") ?? ""),
      description: String(formData.get("description") ?? ""),
      foodCategory: String(formData.get("foodCategory") ?? "OTHER"),
      quantityValue,
      quantityUnit: String(formData.get("quantityUnit") ?? "SERVINGS"),
      estimatedServings,
      expiryTime,
      pickupStartTime,
      pickupEndTime,
      latitude,
      longitude,
    });
    redirect(`/food-listings/${listing.id}`);
  } catch (err) {
    if (err instanceof ApiError) {
      return { error: `${err.code}: ${err.message}` };
    }
    throw err;
  }
}
