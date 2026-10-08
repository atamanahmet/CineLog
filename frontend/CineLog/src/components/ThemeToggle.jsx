import { Moon, Sun } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useUserPrefsStore } from "../stores/userPrefsStore";

/**
 * Theme toggle shared by the desktop navbar and the mobile Sheet.
 */
export default function ThemeToggle({ className }) {
  const theme = useUserPrefsStore((s) => s.theme);
  const toggleTheme = useUserPrefsStore((s) => s.toggleTheme);

  return (
    <Button
      variant="ghost"
      size="icon"
      type="button"
      onClick={toggleTheme}
      aria-label="Toggle dark mode"
      className={className}
    >
      {theme === "dark" ? (
        <Sun className="h-5 w-5" />
      ) : (
        <Moon className="h-5 w-5" />
      )}
    </Button>
  );
}
