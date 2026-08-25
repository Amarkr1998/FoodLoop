"use client";

import { useActionState } from "react";
import { Button, Stack } from "@foodloop/ui";
import { createFoodListing, type CreateListingState } from "./actions";

const initialState: CreateListingState = {};

const CATEGORIES = ["COOKED_MEAL", "PACKAGED", "PRODUCE", "BAKERY", "DAIRY", "BEVERAGE", "OTHER"];
const UNITS = ["SERVINGS", "KG", "BOXES", "LITERS", "PIECES"];

export function NewListingForm({ donorOrgId }: { donorOrgId: string }) {
  const boundAction = createFoodListing.bind(null, donorOrgId);
  const [state, formAction, pending] = useActionState(boundAction, initialState);

  return (
    <form action={formAction}>
      <Stack gap="md">
        <label>
          Title
          <input name="title" type="text" required style={{ width: "100%" }} />
        </label>
        <label>
          Description
          <textarea name="description" style={{ width: "100%" }} />
        </label>
        <label>
          Category
          <select name="foodCategory" defaultValue="COOKED_MEAL" style={{ width: "100%" }}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>
        <Stack direction="row" gap="md">
          <label style={{ flex: 1 }}>
            Quantity
            <input name="quantityValue" type="number" step="0.1" required style={{ width: "100%" }} />
          </label>
          <label style={{ flex: 1 }}>
            Unit
            <select name="quantityUnit" defaultValue="SERVINGS" style={{ width: "100%" }}>
              {UNITS.map((u) => (
                <option key={u} value={u}>
                  {u}
                </option>
              ))}
            </select>
          </label>
        </Stack>
        <label>
          Estimated servings
          <input name="estimatedServings" type="number" style={{ width: "100%" }} />
        </label>
        <label>
          Expires at
          <input name="expiryTime" type="datetime-local" required style={{ width: "100%" }} />
        </label>
        <Stack direction="row" gap="md">
          <label style={{ flex: 1 }}>
            Pickup window start
            <input name="pickupStartTime" type="datetime-local" required style={{ width: "100%" }} />
          </label>
          <label style={{ flex: 1 }}>
            Pickup window end
            <input name="pickupEndTime" type="datetime-local" required style={{ width: "100%" }} />
          </label>
        </Stack>
        <Stack direction="row" gap="md">
          <label style={{ flex: 1 }}>
            Latitude
            <input name="latitude" type="number" step="0.000001" required style={{ width: "100%" }} />
          </label>
          <label style={{ flex: 1 }}>
            Longitude
            <input name="longitude" type="number" step="0.000001" required style={{ width: "100%" }} />
          </label>
        </Stack>
        {state.error && <p style={{ color: "var(--color-danger-500)" }}>{state.error}</p>}
        <Button type="submit" disabled={pending}>
          {pending ? "Saving..." : "Create listing (draft)"}
        </Button>
      </Stack>
    </form>
  );
}
