import { useState } from "react";
import { Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import useAccount from "../hooks/useAccount";
import { getApiErrorMessage } from "../lib/apiError";
import FormField from "./FormField";
import SettingsSection from "./SettingsSection";
import StatusMessage from "./StatusMessage";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function AccountSection() {
  const { accountQuery, emailMutation } = useAccount();
  const [email, setEmail] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [status, setStatus] = useState(null);

  const account = accountQuery.data;
  const emailValid = EMAIL_PATTERN.test(email.trim());
  const canSubmit = emailValid && currentPassword.length > 0 && !emailMutation.isPending;

  const handleSubmit = (e) => {
    e.preventDefault();
    setStatus(null);
    emailMutation.mutate(
      { email: email.trim(), currentPassword },
      {
        onSuccess: () => {
          setEmail("");
          setCurrentPassword("");
          setStatus({ type: "success", message: "Email updated." });
        },
        onError: (err) =>
          setStatus({ type: "error", message: getApiErrorMessage(err, "Could not update email.") }),
      },
    );
  };

  return (
    <SettingsSection
      id="account"
      icon={Mail}
      title="Account"
      description="Username and the email linked to your account."
    >
      <dl className="mb-6 grid gap-4 sm:grid-cols-2">
        <div className="rounded-xl border border-border bg-secondary/40 px-4 py-3">
          <dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            Username
          </dt>
          <dd className="mt-1 truncate font-medium text-card-foreground">
            {accountQuery.isLoading ? (
              <Skeleton className="h-5 w-32" />
            ) : (
              account?.username ?? "—"
            )}
          </dd>
        </div>
        <div className="rounded-xl border border-border bg-secondary/40 px-4 py-3">
          <dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            Email
          </dt>
          <dd className="mt-1 truncate font-medium text-card-foreground">
            {accountQuery.isLoading ? (
              <Skeleton className="h-5 w-40" />
            ) : (
              account?.email || "Not set"
            )}
          </dd>
        </div>
      </dl>

      <form onSubmit={handleSubmit} className="space-y-4">
        <h3 className="text-sm font-semibold text-card-foreground">
          {account?.email ? "Change email" : "Add email"}
        </h3>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField
            id="new-email"
            label="New email"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <FormField
            id="email-current-password"
            label="Current password"
            type="password"
            autoComplete="current-password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
          />
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <StatusMessage status={status} />
          <Button type="submit" disabled={!canSubmit} className="ml-auto">
            {emailMutation.isPending ? "Saving..." : "Save email"}
          </Button>
        </div>
      </form>
    </SettingsSection>
  );
}
