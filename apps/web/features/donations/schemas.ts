import { z } from "zod";

export const FOOD_CATEGORIES = ["COOKED_MEAL", "PACKAGED", "PRODUCE", "BAKERY", "DAIRY", "BEVERAGE", "OTHER"] as const;
export const QUANTITY_UNITS = ["SERVINGS", "KG", "BOXES", "LITERS", "PIECES"] as const;

export const createDonationSchema = z
  .object({
    title: z.string().min(1, "Title is required").max(200),
    description: z.string().max(2000).optional(),
    foodCategory: z.enum(FOOD_CATEGORIES),
    quantityValue: z.number().positive("Must be greater than 0"),
    quantityUnit: z.enum(QUANTITY_UNITS),
    estimatedServings: z.number().int().positive().optional(),
    expiryTime: z.string().min(1, "Expiry time is required"),
    pickupStartTime: z.string().min(1, "Pickup start is required"),
    pickupEndTime: z.string().min(1, "Pickup end is required"),
    latitude: z.number().min(-90).max(90),
    longitude: z.number().min(-180).max(180),
  })
  .refine((v) => new Date(v.pickupEndTime) > new Date(v.pickupStartTime), {
    message: "Pickup end must be after pickup start",
    path: ["pickupEndTime"],
  })
  .refine((v) => new Date(v.expiryTime) > new Date(), {
    message: "Expiry time must be in the future",
    path: ["expiryTime"],
  });

export type CreateDonationValues = z.infer<typeof createDonationSchema>;
