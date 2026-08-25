/** No GET /agent-runs list/detail endpoint exists — ai-orchestration.yaml only exposes trigger actions and escalation resolution. */
export const mockAgentStatus = [
  { name: "Trust & Safety Agent", state: "Idle", lastRunAgo: "4 min ago" },
  { name: "Food Rescue Agent", state: "Idle", lastRunAgo: "12 min ago" },
  { name: "Matching Agent", state: "Idle", lastRunAgo: "2 min ago" },
  { name: "Food Intelligence Agent", state: "Idle", lastRunAgo: "9 min ago" },
];

export const mockRecentDecisions = [
  { id: "r1", agent: "Matching Agent", summary: "Proposed match: Sunrise Cafe surplus → Hope Shelter", escalated: false, when: "3 min ago" },
  { id: "r2", agent: "Trust & Safety Agent", summary: "Low-risk assessment for new donor account", escalated: false, when: "8 min ago" },
  { id: "r3", agent: "Food Rescue Agent", summary: "T-1h rescue check escalated — no receiver within radius", escalated: true, when: "22 min ago" },
];
