import { ShieldAlert } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import AgeGateDialog from "../../../components/AgeGateDialog";
import useAdultPolicy from "../../../hooks/useAdultPolicy";
import useAgeGate from "../../../hooks/useAgeGate";
import { useUserPrefsStore } from "../../../stores/userPrefsStore";
import SettingsSection from "./SettingsSection";

/**
 * One switch row. Turning on goes through the age gate; turning off is immediate.
 */
function GatedSwitchRow({ id, title, description, checked, onChange, requestConfirmation }) {
  const handleChange = (next) => {
    if (!next) {
      onChange(false);
      return;
    }
    requestConfirmation(() => onChange(true));
  };

  return (
    <div className="flex items-center justify-between gap-4 py-4 first:pt-0 last:pb-0">
      <div>
        <label htmlFor={id} className="text-sm font-medium text-card-foreground">
          {title}
        </label>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      <Switch id={id} checked={checked} onCheckedChange={handleChange} />
    </div>
  );
}

/**
 * Adult-content settings. Hidden entirely when the site disables adult content.
 */
export default function MatureContentSection() {
  const { enabled, includeAdult } = useAdultPolicy();
  const showAdultUnblurred = useUserPrefsStore((s) => s.showAdultUnblurred);
  const setIncludeAdult = useUserPrefsStore((s) => s.setIncludeAdult);
  const setShowAdultUnblurred = useUserPrefsStore((s) => s.setShowAdultUnblurred);
  const { requestConfirmation, dialogProps } = useAgeGate();

  if (!enabled) {
    return null;
  }

  return (
    <SettingsSection
      id="mature"
      icon={ShieldAlert}
      title="Mature content"
      description="Adult titles are hidden and their images blurred by default."
    >
      <div className="divide-y divide-border">
        <GatedSwitchRow
          id="include-adult"
          title="Include adult titles"
          description="Show adult results on Discover, Top, and New pages."
          checked={includeAdult}
          onChange={setIncludeAdult}
          requestConfirmation={requestConfirmation}
        />
        <GatedSwitchRow
          id="unblur-adult"
          title="Show adult images without blur"
          description="Posters and backdrops flagged as adult appear unblurred."
          checked={showAdultUnblurred}
          onChange={setShowAdultUnblurred}
          requestConfirmation={requestConfirmation}
        />
      </div>
      <AgeGateDialog {...dialogProps} />
    </SettingsSection>
  );
}
