import { Cpu } from "lucide-react";
import { TrustAssessmentTrigger } from "@/features/ai-command-center/components/TrustAssessmentTrigger";
import { MatchSuggestionTrigger } from "@/features/ai-command-center/components/MatchSuggestionTrigger";
import { FoodAnalysisTrigger } from "@/features/ai-command-center/components/FoodAnalysisTrigger";
import { EscalationResolver } from "@/features/ai-command-center/components/EscalationResolver";
import { AgentStatusPanel } from "@/features/ai-command-center/components/AgentStatusPanel";

export default function AiCommandCenterPage() {
  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="flex items-center gap-2 text-xl font-semibold tracking-tight">
          <Cpu className="size-5 text-primary" />
          AI Command Center
        </h1>
        <p className="text-sm text-muted-foreground">
          Trigger FoodLoop&apos;s AI agents directly and resolve their escalations.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <TrustAssessmentTrigger />
          <MatchSuggestionTrigger />
          <FoodAnalysisTrigger />
          <EscalationResolver />
        </div>
        <AgentStatusPanel />
      </div>
    </div>
  );
}
