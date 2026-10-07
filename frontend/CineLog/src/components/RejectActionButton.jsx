import { Undo2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";

/**
 * Hide this title, or undo when active on the Not interested tab.
 */
export default function RejectActionButton({ onReject, active = false }) {
  const label = active ? "Remove from Not interested" : "Not interested";
  const Icon = active ? Undo2 : X;

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          type="button"
          size="icon"
          variant={active ? "secondary" : undefined}
          aria-label={label}
          className={
            active
              ? undefined
              : "bg-destructive/80 hover:bg-destructive text-destructive-foreground"
          }
          onClick={(e) => {
            e.stopPropagation();
            onReject?.();
          }}
        >
          <Icon />
        </Button>
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  );
}
