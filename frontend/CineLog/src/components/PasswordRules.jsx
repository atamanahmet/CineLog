import { isPasswordTooLong, passwordRules } from "../utils/passwordRules";

/**
 * Check icon for a met password rule.
 */
function CheckIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-4 w-4 shrink-0"
      fill="none"
      viewBox="0 0 16 16"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M3 8.5 6.5 12 13 4.5"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </svg>
  );
}

/**
 * Cross icon for an unmet password rule.
 */
function CrossIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-4 w-4 shrink-0"
      fill="none"
      viewBox="0 0 16 16"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M4 4 12 12M12 4 4 12"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="2"
      />
    </svg>
  );
}

/**
 * Live password rule checklist. Hidden until the field is focused or not empty.
 */
export default function PasswordRules({ password, focused }) {
  const value = password ?? "";
  if (!focused && value.length === 0) {
    return null;
  }

  return (
    <ul aria-live="polite" className="mt-2 space-y-1 text-sm">
      {passwordRules.map((rule) => {
        const met = rule.test(value);
        return (
          <li
            key={rule.id}
            className={`flex items-center gap-2 ${met ? "text-green-400" : "text-red-400"}`}
          >
            {met ? <CheckIcon /> : <CrossIcon />}
            <span>{rule.label}</span>
          </li>
        );
      })}
      {isPasswordTooLong(value) ? (
        <li className="flex items-center gap-2 text-red-400">
          <CrossIcon />
          <span>Password is too long</span>
        </li>
      ) : null}
    </ul>
  );
}
