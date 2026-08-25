"use client";

import { Truck, User } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";
import { useAvailablePickups, useDelayedPickups, useClaimPickup } from "@/features/pickups/api";
import { PickupCard } from "@/features/pickups/components/PickupCard";
import { KanbanColumn } from "@/features/pickups/components/KanbanColumn";
import { mockAssigned, mockCompleted } from "@/features/pickups/mock-data";
import { useGeolocation } from "@/lib/use-geolocation";

export default function PickupsPage() {
  const { coords } = useGeolocation();
  const { data: available, isLoading: availableLoading } = useAvailablePickups(coords.lat, coords.lng);
  const { data: delayed, isLoading: delayedLoading } = useDelayedPickups();
  const claimPickup = useClaimPickup();

  const pendingTasks = available?.content ?? [];
  const delayedTasks = delayed ?? [];

  return (
    <div className="flex h-full flex-col space-y-6 p-6">
      <div>
        <h1 className="flex items-center gap-2 text-xl font-semibold tracking-tight">
          <Truck className="size-5 text-primary" />
          Pickup operations
        </h1>
        <p className="text-sm text-muted-foreground">Live pickup task board.</p>
      </div>

      <div className="flex flex-1 gap-4 overflow-x-auto pb-2">
        <KanbanColumn title="Pending" count={pendingTasks.length} accent="warning">
          {availableLoading ? (
            <Skeleton className="h-24 w-full" />
          ) : pendingTasks.length === 0 ? (
            <EmptyColumn />
          ) : (
            pendingTasks.map((task) => (
              <PickupCard
                key={task.id}
                task={task}
                actionLabel="Claim"
                pending={claimPickup.isPending}
                onAction={() => task.id && claimPickup.mutate(task.id)}
              />
            ))
          )}
        </KanbanColumn>

        <KanbanColumn title="Delayed" count={delayedTasks.length} accent="destructive">
          {delayedLoading ? (
            <Skeleton className="h-24 w-full" />
          ) : delayedTasks.length === 0 ? (
            <EmptyColumn />
          ) : (
            delayedTasks.map((task) => <PickupCard key={task.id} task={task} />)
          )}
        </KanbanColumn>

        <KanbanColumn title="En route" count={mockAssigned.length} accent="info" preview>
          {mockAssigned.map((c) => (
            <PreviewCard key={c.id} name={c.volunteerName} sub={`ETA ${c.eta}`} />
          ))}
        </KanbanColumn>

        <KanbanColumn title="Completed" count={mockCompleted.length} accent="success" preview>
          {mockCompleted.map((c) => (
            <PreviewCard key={c.id} name={c.volunteerName} sub={c.eta} />
          ))}
        </KanbanColumn>
      </div>
    </div>
  );
}

function EmptyColumn() {
  return <p className="py-8 text-center text-xs text-muted-foreground">Nothing here right now.</p>;
}

function PreviewCard({ name, sub }: { name: string; sub: string }) {
  return (
    <Card className="shadow-none">
      <CardContent className="flex items-center gap-2 px-4 py-3">
        <User className="size-3.5 text-muted-foreground" />
        <div>
          <p className="text-sm font-medium">{name}</p>
          <p className="text-xs text-muted-foreground">{sub}</p>
        </div>
      </CardContent>
    </Card>
  );
}
