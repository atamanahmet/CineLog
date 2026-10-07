/**
 * Profile tab label with optional count badge for Tabs and Select.
 */
export default function ProfileTabLabel({ label, count }) {
  return (
    <>
      <span>{label}</span>
      {count != null && (
        <span className="inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1.5 text-[10px] font-semibold text-primary-foreground group-data-[state=active]:bg-background group-data-[state=active]:text-foreground group-data-[state=checked]:bg-background group-data-[state=checked]:text-foreground">
          {count}
        </span>
      )}
    </>
  );
}
