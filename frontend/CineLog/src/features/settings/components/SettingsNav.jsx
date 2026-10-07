import {
  KeyRound,
  Mail,
  ShieldAlert,
  SlidersHorizontal,
  Sparkles,
  UserRound,
} from "lucide-react";
import useAdultPolicy from "../../../hooks/useAdultPolicy";

const SETTINGS_SECTIONS = [
  { id: "profile", label: "Profile", icon: UserRound },
  { id: "account", label: "Account", icon: Mail },
  { id: "security", label: "Password", icon: KeyRound },
  { id: "preferences", label: "Preferences", icon: SlidersHorizontal },
  { id: "mature", label: "Mature content", icon: ShieldAlert, adultOnly: true },
  { id: "recommendations", label: "Recommendations", icon: Sparkles },
];

/**
 * Section jump links. Horizontal scroller on mobile, sticky column on desktop.
 */
export default function SettingsNav() {
  const { enabled: adultEnabled } = useAdultPolicy();
  const sections = SETTINGS_SECTIONS.filter((s) => adultEnabled || !s.adultOnly);

  return (
    <nav aria-label="Settings sections" className="lg:sticky lg:top-[calc(var(--header-height)+1.5rem)]">
      <ul className="-mx-1 flex gap-1 overflow-x-auto pb-1 lg:mx-0 lg:flex-col lg:overflow-visible lg:pb-0">
        {sections.map((section) => {
          const Icon = section.icon;
          return (
            <li key={section.id} className="shrink-0">
              <a
                href={`#${section.id}`}
                className="flex items-center gap-2.5 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
              >
                <Icon className="size-4" />
                {section.label}
              </a>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
