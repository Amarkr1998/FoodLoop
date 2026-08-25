/** No NGO capacity/demand or claim-history endpoint exists yet — see ngo.yaml (verification-request + ngo-requests only). */
export const mockCapacity = { current: 68, max: 100 };
export const mockDemand = [
  { category: "COOKED_MEAL", demand: "High" },
  { category: "PRODUCE", demand: "Medium" },
  { category: "BAKERY", demand: "Low" },
];
export const mockPickupHistory = [
  { id: "h1", title: "Sunrise Cafe surplus", completedAt: "Yesterday, 4:20 PM" },
  { id: "h2", title: "Green Valley Bakery bread", completedAt: "2 days ago" },
  { id: "h3", title: "Fresh Mart produce", completedAt: "3 days ago" },
];
