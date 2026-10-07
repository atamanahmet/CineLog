import { cn } from "@/lib/utils";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

/**
 * Shared sort dropdown on shadcn Select. Width comes from parent className.
 */
export default function SortSelect({
  value,
  onValueChange,
  options,
  ariaLabel,
  className,
  disabled = false,
  placeholder = "Sort by",
}) {
  return (
    <Select value={value} onValueChange={onValueChange} disabled={disabled}>
      <SelectTrigger
        aria-label={ariaLabel}
        className={cn(
          "h-9 rounded-md border border-border bg-card text-sm font-medium text-foreground shadow-none",
          className,
        )}
      >
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent
        position="popper"
        className="w-[var(--radix-select-trigger-width)] rounded-md border border-border bg-popover text-popover-foreground shadow-md"
      >
        {options.map((opt) => (
          <SelectItem
            key={opt.value}
            value={opt.value}
            className="group min-h-11 cursor-pointer py-2.5"
          >
            {opt.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
