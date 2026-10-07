import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "../../../stores/authStore";
import { fetchAccount, updateEmail, updatePassword } from "../api/accountApi";

const ACCOUNT_KEY = ["account"];

/**
 * Account details plus email and password mutations for the logged-in user.
 */
export default function useAccount() {
  const user = useAuthStore((s) => s.user);
  const queryClient = useQueryClient();

  const accountQuery = useQuery({
    queryKey: ACCOUNT_KEY,
    queryFn: fetchAccount,
    enabled: Boolean(user),
  });

  const emailMutation = useMutation({
    mutationFn: updateEmail,
    onSuccess: (account) => queryClient.setQueryData(ACCOUNT_KEY, account),
  });

  const passwordMutation = useMutation({ mutationFn: updatePassword });

  return { accountQuery, emailMutation, passwordMutation };
}
