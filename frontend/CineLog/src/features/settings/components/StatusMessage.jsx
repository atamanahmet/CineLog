import { AlertCircle, CheckCircle2 } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Inline success or error line under a form.
 */
export default function StatusMessage({ status }) {
  if (!status?.message) {
    return null;
  }
  const isError = status.type === "error";
  const Icon = isError ? AlertCircle : CheckCircle2;

  return (
    <p
      role={isError ? "alert" : "status"}
      className={cn(
        "flex items-center gap-2 text-sm",
        isError ? "text-destructive" : "text-green-600 dark:text-green-400",
      )}
    >
      <Icon className="size-4 shrink-0" />
      {status.message}
    </p>
  );
}
