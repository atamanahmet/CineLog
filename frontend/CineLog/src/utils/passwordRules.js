/**
 * These rules must match the backend StrongPasswordValidator.
 */

const UTF8_ENCODER = new TextEncoder();

export const passwordRules = [
  {
    id: "minLength",
    label: "At least 8 characters",
    test: (password) => Array.from(password ?? "").length >= 8,
  },
  {
    id: "uppercase",
    label: "An uppercase letter",
    test: (password) => /\p{Lu}/u.test(password ?? ""),
  },
  {
    id: "lowercase",
    label: "A lowercase letter",
    test: (password) => /\p{Ll}/u.test(password ?? ""),
  },
  {
    id: "digit",
    label: "A number",
    test: (password) => /\p{Nd}/u.test(password ?? ""),
  },
  {
    id: "special",
    label: "A special character",
    test: (password) => /[^\p{L}\p{Nd}\s]/u.test(password ?? ""),
  },
];

/**
 * True when every checklist rule passes.
 */
export function allPasswordRulesPass(password) {
  return passwordRules.every((rule) => rule.test(password ?? ""));
}

/**
 * True when the password is over 72 bytes in UTF-8.
 */
export function isPasswordTooLong(password) {
  return UTF8_ENCODER.encode(password ?? "").length > 72;
}
