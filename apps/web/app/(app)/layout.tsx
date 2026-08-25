import type { ReactNode } from "react";
import { Sidebar } from "@/features/shell/components/Sidebar";
import { Header } from "@/features/shell/components/Header";
import { CommandPalette } from "@/features/shell/components/CommandPalette";

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header />
        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
      <CommandPalette />
    </div>
  );
}
