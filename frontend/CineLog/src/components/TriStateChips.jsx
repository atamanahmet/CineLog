import { Check, Ban } from "lucide-react";
import { Button } from "@/components/ui/button";

/**
 * Tri-state chips. Tap: off → include → exclude → off.
 * chips: { value, label }. Width and facet title come from the parent.
 */
export default function TriStateChips({
  title,
  chips,
  filter,
  onCycle,
  helperText = "Tap to include, tap again to exclude, once more to clear.",
}) {
  const include = new Set(filter?.include ?? []);
  const exclude = new Set(filter?.exclude ?? []);
  const list = Array.isArray(chips) ? chips : [];

  return (
    <section className="min-w-0">
      {title ? (
        <h3 className="flex items-center gap-2 px-1 pb-1 text-sm font-medium text-foreground/80">
          {title}
        </h3>
      ) : null}
      {helperText ? (
        <p className="px-1 pb-2 text-xs text-muted-foreground">{helperText}</p>
      ) : null}
      <div className="flex min-w-0 flex-wrap gap-2 px-1">
        {list.map((chip) => {
          const state = include.has(chip.value)
            ? "include"
            : exclude.has(chip.value)
              ? "exclude"
              : "off";
          const aria =
            state === "include"
              ? `${chip.label}, included. Tap to exclude`
              : state === "exclude"
                ? `${chip.label}, excluded. Tap to clear`
                : `${chip.label}, off. Tap to include`;
          return (
            <Button
              key={String(chip.value)}
              type="button"
              size="sm"
              variant={
                state === "include"
                  ? "default"
                  : state === "exclude"
                    ? "outline"
                    : "outline"
              }
              aria-label={aria}
              aria-pressed={state !== "off"}
              onClick={() => onCycle(chip.value)}
              className={
                state === "exclude"
                  ? "border-destructive text-destructive line-through hover:bg-destructive/10 hover:text-destructive"
                  : undefined
              }
            >
              {state === "include" ? <Check className="size-3.5" /> : null}
              {state === "exclude" ? <Ban className="size-3.5" /> : null}
              {chip.label}
            </Button>
          );
        })}
      </div>
    </section>
  );
}
