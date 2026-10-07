import { Button } from "@/components/ui/button";
import AccountSection from "../features/settings/components/AccountSection";
import MatureContentSection from "../features/settings/components/MatureContentSection";
import PasswordSection from "../features/settings/components/PasswordSection";
import PreferencesSection from "../features/settings/components/PreferencesSection";
import ProfilePhotoSection from "../features/settings/components/ProfilePhotoSection";
import RecommendationSection from "../features/settings/components/RecommendationSection";
import SettingsNav from "../features/settings/components/SettingsNav";
import { useAuthModalStore } from "../stores/authModalStore";
import { useAuthStore } from "../stores/authStore";

export default function SettingsPage() {
  const user = useAuthStore((s) => s.user);
  const openModal = useAuthModalStore((s) => s.openModal);

  if (!user) {
    return (
      <div className="page-container flex flex-col items-center gap-4 py-24 text-center">
        <h1 className="text-2xl font-semibold text-foreground">Sign in to manage settings</h1>
        <p className="text-muted-foreground">Your account, photo, and preferences live here.</p>
        <Button onClick={() => openModal("login")}>Log in</Button>
      </div>
    );
  }

  return (
    <div className="page-container py-10">
      <header className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-foreground">Settings</h1>
        <p className="mt-1 text-muted-foreground">
          Manage your profile, account security, and how CineLog works for you.
        </p>
      </header>

      <div className="grid gap-8 lg:grid-cols-[13rem_minmax(0,1fr)]">
        <aside>
          <SettingsNav />
        </aside>
        <div className="flex max-w-3xl flex-col gap-6">
          <ProfilePhotoSection />
          <AccountSection />
          <PasswordSection />
          <PreferencesSection />
          <MatureContentSection />
          <RecommendationSection />
        </div>
      </div>
    </div>
  );
}
