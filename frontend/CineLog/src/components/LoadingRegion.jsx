import { cn } from "@/lib/utils";

/**
 * Accessible loading wrapper. Skeletons inside should be aria-hidden.
 */
export default function LoadingRegion({
  children,
  className,
  label = "Loading",
}) {
  return (
    <div aria-busy="true" className={cn(className)}>
      <span className="sr-only">{label}</span>
      {children}
    </div>
  );
}
