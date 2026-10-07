import { useListsStore } from "../stores/listsStore";
import { usePageFiltersStore } from "../stores/pageFiltersStore";
import { useSimilarStore } from "../stores/similarStore";

/**
 * Drop list, similar, filter, and rejected caches after logout or session wipe.
 */
export function clearSessionCaches(queryClient) {
  useListsStore.getState().resetLists();
  useSimilarStore.getState().reset();
  usePageFiltersStore.getState().clearAll();
  queryClient.removeQueries({ queryKey: ["similar"] });
  queryClient.removeQueries({ queryKey: ["similar-search"] });
  queryClient.removeQueries({ queryKey: ["rejected"] });
}
