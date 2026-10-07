import { useState } from "react";
import { KeyRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import PasswordRules from "../../../components/PasswordRules";
import { allPasswordRulesPass, isPasswordTooLong } from "../../../utils/passwordRules";
import useAccount from "../hooks/useAccount";
import { getApiErrorMessage } from "../lib/apiError";
import FormField from "./FormField";
import SettingsSection from "./SettingsSection";
import StatusMessage from "./StatusMessage";

const EMPTY_FORM = { currentPassword: "", newPassword: "", confirmPassword: "" };

export default function PasswordSection() {
  const { passwordMutation } = useAccount();
  const [form, setForm] = useState(EMPTY_FORM);
  const [newFocused, setNewFocused] = useState(false);
  const [status, setStatus] = useState(null);

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const strong = allPasswordRulesPass(form.newPassword) && !isPasswordTooLong(form.newPassword);
  const matches = form.newPassword === form.confirmPassword;
  const showMismatch = form.confirmPassword.length > 0 && !matches;
  const canSubmit =
    form.currentPassword.length > 0 && strong && matches && !passwordMutation.isPending;

  const handleSubmit = (e) => {
    e.preventDefault();
    setStatus(null);
    passwordMutation.mutate(
      { currentPassword: form.currentPassword, newPassword: form.newPassword },
      {
        onSuccess: () => {
          setForm(EMPTY_FORM);
          setStatus({
            type: "success",
            message: "Password updated. Other devices were signed out.",
          });
        },
        onError: (err) =>
          setStatus({
            type: "error",
            message: getApiErrorMessage(err, "Could not update password."),
          }),
      },
    );
  };

  return (
    <SettingsSection
      id="security"
      icon={KeyRound}
      title="Password"
      description="Changing your password signs you out everywhere else."
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <FormField
          id="current-password"
          label="Current password"
          type="password"
          autoComplete="current-password"
          value={form.currentPassword}
          onChange={update("currentPassword")}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField
            id="new-password"
            label="New password"
            type="password"
            autoComplete="new-password"
            value={form.newPassword}
            onChange={update("newPassword")}
            onFocus={() => setNewFocused(true)}
            onBlur={() => setNewFocused(false)}
          >
            <PasswordRules password={form.newPassword} focused={newFocused} />
          </FormField>
          <FormField
            id="confirm-password"
            label="Confirm new password"
            type="password"
            autoComplete="new-password"
            value={form.confirmPassword}
            onChange={update("confirmPassword")}
          >
            {showMismatch && (
              <p className="text-sm text-destructive">Passwords do not match</p>
            )}
          </FormField>
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <StatusMessage status={status} />
          <Button type="submit" disabled={!canSubmit} className="ml-auto">
            {passwordMutation.isPending ? "Updating..." : "Update password"}
          </Button>
        </div>
      </form>
    </SettingsSection>
  );
}
