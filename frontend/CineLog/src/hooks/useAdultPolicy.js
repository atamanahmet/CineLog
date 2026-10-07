import { useUserPrefsStore } from "../stores/userPrefsStore";
import useSiteConfig from "./useSiteConfig";

/**
 * Combine the server guardrail with user prefs. Fails closed: while config
 * is loading or unavailable, adult options are hidden and posters stay blurred.
 */
export default function useAdultPolicy() {
  const { data } = useSiteConfig();
  const includeAdultPref = useUserPrefsStore((s) => s.includeAdult);
  const showUnblurredPref = useUserPrefsStore((s) => s.showAdultUnblurred);

  const enabled = data?.adultContentEnabled === true;

  return {
    enabled,
    includeAdult: enabled && Boolean(includeAdultPref),
    blurAdult: !(enabled && showUnblurredPref),
  };
}
