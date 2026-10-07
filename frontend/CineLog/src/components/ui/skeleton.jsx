import { cn } from "@/lib/utils"

/**
 * Theme-token skeleton block. Pass shell for card frames (blur + border + sweep).
 */
function Skeleton({
  className,
  shell = false,
  ...props
}) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        "relative overflow-hidden rounded-md bg-muted/50",
        "animate-pulse motion-reduce:animate-none",
        shell && "border border-border/40 backdrop-blur-sm skeleton-sweep",
        className,
      )}
      {...props}
    />
  )
}

export { Skeleton }
