import { Film, Moon, SlidersHorizontal, Sun, Tv } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { useUserPrefsStore } from "../../../stores/userPrefsStore";
import SegmentedControl from "./SegmentedControl";
import SettingsSection from "./SettingsSection";

const THEME_OPTIONS = [
  { value: "light", label: "Light", icon: Sun },
  { value: "dark", label: "Dark", icon: Moon },
];

const MEDIA_OPTIONS = [
  { value: "movie", label: "Movies", icon: Film },
  { value: "tv", label: "TV Shows", icon: Tv },
];

/**
 * One label/description row with a control on the right.
 */
function PreferenceRow({ title, description, children }) {
  return (
    <div className="flex flex-col gap-3 py-4 first:pt-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="text-sm font-medium text-card-foreground">{title}</p>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      {children}
    </div>
  );
}

export default function PreferencesSection() {
  const theme = useUserPrefsStore((s) => s.theme);
  const setTheme = useUserPrefsStore((s) => s.setTheme);
  const mediaType = useUserPrefsStore((s) => s.mediaType);
  const setMediaType = useUserPrefsStore((s) => s.setMediaType);
  const filterPanelOpen = useUserPrefsStore((s) => s.filterPanelOpen);
  const setFilterPanelOpen = useUserPrefsStore((s) => s.setFilterPanelOpen);
  const resetPreferences = useUserPrefsStore((s) => s.resetPreferences);

  return (
    <SettingsSection
      id="preferences"
      icon={SlidersHorizontal}
      title="Preferences"
      description="Saved on this device and applied right away."
      footer={
        <Button variant="ghost" onClick={resetPreferences}>
          Reset to defaults
        </Button>
      }
    >
      <div className="divide-y divide-border">
        <PreferenceRow title="Theme" description="Light or dark interface.">
          <SegmentedControl
            ariaLabel="Theme"
            options={THEME_OPTIONS}
            value={theme}
            onChange={setTheme}
          />
        </PreferenceRow>
        <PreferenceRow
          title="Content type"
          description="What Discover, Top, and New pages show."
        >
          <SegmentedControl
            ariaLabel="Content type"
            options={MEDIA_OPTIONS}
            value={mediaType}
            onChange={setMediaType}
          />
        </PreferenceRow>
        <PreferenceRow
          title="Keep filter panel open"
          description="Show the filter panel expanded on catalog pages."
        >
          <Switch
            checked={filterPanelOpen}
            onCheckedChange={setFilterPanelOpen}
            aria-label="Keep filter panel open"
          />
        </PreferenceRow>
      </div>
    </SettingsSection>
  );
}
