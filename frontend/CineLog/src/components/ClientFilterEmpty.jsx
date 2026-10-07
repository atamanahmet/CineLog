import { Button } from "@/components/ui/button";

export const CLIENT_FILTER_EMPTY_TEXT = "No titles match these filters.";

/**
 * Shared empty state when client genre/language filters wipe the visible list.
 */
export default function ClientFilterEmpty({ onClear, className }) {
  return (
    <div
      className={
        className ??
        "flex w-full flex-col items-center gap-3 py-10 text-center"
      }
    >
      <p className="text-foreground">{CLIENT_FILTER_EMPTY_TEXT}</p>
      <Button type="button" variant="outline" onClick={onClear}>
        Clear filters
      </Button>
    </div>
  );
}
