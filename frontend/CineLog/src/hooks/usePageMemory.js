import { useEffect } from "react";
import { usePageFiltersStore } from "../stores/pageFiltersStore";

/**
 * Lifecycle only. Marks a filter scope entered on mount and left on unmount.
 */
export default function usePageMemory(scopeKey) {
  const markEntered = usePageFiltersStore((s) => s.markEntered);
  const markLeft = usePageFiltersStore((s) => s.markLeft);

  useEffect(() => {
    if (scopeKey == null) {
      return undefined;
    }
    markEntered(scopeKey, Date.now());
    return () => {
      markLeft(scopeKey, Date.now());
    };
  }, [scopeKey, markEntered, markLeft]);
}
