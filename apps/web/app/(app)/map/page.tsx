import { LiveMap } from "@/features/map/components/LiveMap";

export default function MapPage() {
  return (
    <div className="h-[calc(100vh-3.5rem)] w-full">
      <LiveMap />
    </div>
  );
}
