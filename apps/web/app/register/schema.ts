import { z } from "zod";

export const registerSchema = z.object({
  displayName: z.string().min(1, "Display name is required"),
  email: z.string().email("Enter a valid email address"),
  password: z.string().min(8, "Password must be at least 8 characters"),
});

export type RegisterValues = z.infer<typeof registerSchema>;
