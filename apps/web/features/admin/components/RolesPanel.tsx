import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { mockRoles } from "../mock-data";

export function RolesPanel() {
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
      {mockRoles.map((r) => (
        <Card key={r.role} className="shadow-none">
          <CardContent className="px-4 py-3">
            <Badge variant="outline" className="mb-1.5">{r.role}</Badge>
            <p className="text-sm text-muted-foreground">{r.description}</p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
