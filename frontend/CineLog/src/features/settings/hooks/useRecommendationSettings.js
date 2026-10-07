import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "../../../stores/authStore";
import { useListsStore } from "../../../stores/listsStore";
import {
  fetchRecommendationSettings,
  updateRecommendationSettings,
} from "../api/accountApi";

const REC_SETTINGS_KEY = ["recommendation-settings"];

/**
 * Server-side recommendation engine settings. Saving refreshes recommendations.
 */
export default function useRecommendationSettings() {
  const user = useAuthStore((s) => s.user);
  const getAllRecommendations = useListsStore((s) => s.getAllRecommendations);
  const queryClient = useQueryClient();

  const settingsQuery = useQuery({
    queryKey: REC_SETTINGS_KEY,
    queryFn: fetchRecommendationSettings,
    enabled: Boolean(user),
  });

  const saveMutation = useMutation({
    mutationFn: updateRecommendationSettings,
    onSuccess: (saved) => {
      queryClient.setQueryData(REC_SETTINGS_KEY, saved);
      getAllRecommendations();
    },
  });

  return { settingsQuery, saveMutation };
}
