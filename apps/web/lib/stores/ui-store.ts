import { create } from "zustand";

/**
 * Pure client/UI state — never server data (that's TanStack Query's job).
 * Kept intentionally small: sidebar collapse and the command palette are
 * the only two pieces of state that (a) need to be read from multiple,
 * unrelated places in the tree and (b) aren't naturally derivable from the
 * URL or a server response.
 */
interface UiState {
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
  commandPaletteOpen: boolean;
  setCommandPaletteOpen: (open: boolean) => void;
}

export const useUiStore = create<UiState>((set) => ({
  sidebarCollapsed: false,
  toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
  commandPaletteOpen: false,
  setCommandPaletteOpen: (open) => set({ commandPaletteOpen: open }),
}));
