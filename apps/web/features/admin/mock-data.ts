/** No admin user list/roles/audit-log/event-stream/health endpoint exists — identity.yaml only exposes /users/me. */
export const mockUsers = [
  { id: "u1", name: "Asha Rao", email: "asha@sunrisecafe.example", roles: ["DONOR"], status: "Active" },
  { id: "u2", name: "Priya Nair", email: "priya@hopeshelter.example", roles: ["NGO_ADMIN"], status: "Active" },
  { id: "u3", name: "Sam Verma", email: "sam@foodloop.example", roles: ["VOLUNTEER"], status: "Active" },
  { id: "u4", name: "Kabir Singh", email: "kabir@foodloop.example", roles: ["ADMIN"], status: "Active" },
];

export const mockRoles = [
  { role: "ADMIN", description: "Full platform access across all tenants." },
  { role: "ORG_ADMIN", description: "Manages a single organization's members and listings." },
  { role: "DONOR", description: "Creates and publishes food donations." },
  { role: "NGO_ADMIN", description: "Claims donations on behalf of a receiving organization." },
  { role: "VOLUNTEER", description: "Claims and fulfills pickups." },
  { role: "TRUST_OPS", description: "Reviews AI trust/safety escalations." },
];

export const mockAuditLog = [
  { id: "a1", actor: "kabir@foodloop.example", action: "Verified organization “Sunrise Cafe”", when: "12 min ago" },
  { id: "a2", actor: "system", action: "Auto-cancelled expired donation “Bakery surplus #4021”", when: "38 min ago" },
  { id: "a3", actor: "priya@hopeshelter.example", action: "Claimed donation “Green Valley produce”", when: "1 hr ago" },
];

export const mockSystemHealth = [
  { service: "api-gateway", status: "Healthy", latencyMs: 42 },
  { service: "food", status: "Healthy", latencyMs: 58 },
  { service: "matching", status: "Healthy", latencyMs: 71 },
  { service: "ai-orchestration", status: "Healthy", latencyMs: 210 },
  { service: "notification", status: "Degraded", latencyMs: 890 },
];
