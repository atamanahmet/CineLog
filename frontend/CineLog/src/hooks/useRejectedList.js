import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchUserList } from "../api/listsApi";
import { omitListedItems } from "../utils/listItems";
import { useListsStore } from "../stores/listsStore";
import { REJECTED_LIST_TYPE } from "./useRejectTitle";
import { REJECTED_QUERY_KEY } from "./useRestoreRejected";

/**
 * Rejected titles for the Not interested tab, minus watchlist and loved.
 */
export default function useRejectedList() {
  const watchlist = useListsStore((s) => s.watchlist);
  const lovedlist = useListsStore((s) => s.lovedlist);

  const query = useQuery({
    queryKey: REJECTED_QUERY_KEY,
    queryFn: () => fetchUserList(REJECTED_LIST_TYPE),
    staleTime: 30_000,
  });

  const items = useMemo(
    () => omitListedItems(query.data ?? [], watchlist, lovedlist),
    [query.data, watchlist, lovedlist],
  );

  return {
    items,
    hasData: query.data != null,
    isLoading: query.isPending || (query.isFetching && query.data == null),
    isError: query.isError,
    refetch: query.refetch,
  };
}
