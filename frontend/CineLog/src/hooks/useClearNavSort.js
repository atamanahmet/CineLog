import { useCallback } from "react";
import { pageKeyFromPath } from "../lib/pagePresets";
import { useUserPrefsStore } from "../stores/userPrefsStore";

/**
 * Drops the stored sort for a nav destination so the page preset sort applies.
 */
export default function useClearNavSort() {
  const setSortForPage = useUserPrefsStore((s) => s.setSortForPage);

  return useCallback(
    (path) => {
      setSortForPage(pageKeyFromPath(path), null);
    },
    [setSortForPage],
  );
}
