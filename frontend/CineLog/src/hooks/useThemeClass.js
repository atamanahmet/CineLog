import { useEffect } from "react";
import { useUserPrefsStore } from "../stores/userPrefsStore";

/**
 * Mirror the persisted theme preference onto the html element class.
 */
export default function useThemeClass() {
  const theme = useUserPrefsStore((s) => s.theme);

  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
  }, [theme]);
}
