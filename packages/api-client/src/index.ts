export { createApiClient, ApiError } from "./client";
export type { ApiClient, ApiClientConfig, RequestOptions } from "./client";

// Generated per-service path/schema types (run `pnpm generate:api-client` first).
export type { paths as IdentityPaths, components as IdentitySchemas } from "./generated/identity";
export type { paths as TenantPaths, components as TenantSchemas } from "./generated/tenant";
export type { paths as FoodPaths, components as FoodSchemas } from "./generated/food";
export type { paths as PickupPaths, components as PickupSchemas } from "./generated/pickup";
export type { paths as AiOrchestrationPaths, components as AiOrchestrationSchemas } from "./generated/ai-orchestration";
export type { paths as MatchingPaths, components as MatchingSchemas } from "./generated/matching";
export type { paths as NotificationPaths, components as NotificationSchemas } from "./generated/notification";
export type { paths as ImpactPaths, components as ImpactSchemas } from "./generated/impact";
export type { paths as NgoPaths, components as NgoSchemas } from "./generated/ngo";
export type { paths as TrustPaths, components as TrustSchemas } from "./generated/trust";
