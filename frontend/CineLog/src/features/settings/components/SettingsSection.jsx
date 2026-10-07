/**
 * Card shell for one settings block: header, body, optional action footer.
 */
export default function SettingsSection({
  id,
  icon: Icon,
  title,
  description,
  footer,
  children,
}) {
  return (
    <section
      id={id}
      className="scroll-mt-[calc(var(--header-height)+1.5rem)] overflow-hidden rounded-2xl border border-border bg-card shadow-sm"
    >
      <header className="flex items-start gap-3 border-b border-border px-6 py-5">
        {Icon && (
          <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-secondary text-primary">
            <Icon className="size-4" />
          </span>
        )}
        <div className="min-w-0">
          <h2 className="text-base font-semibold text-card-foreground">{title}</h2>
          {description && (
            <p className="mt-0.5 text-sm text-muted-foreground">{description}</p>
          )}
        </div>
      </header>
      <div className="px-6 py-5">{children}</div>
      {footer && (
        <footer className="flex flex-wrap items-center justify-end gap-3 border-t border-border bg-secondary/40 px-6 py-3">
          {footer}
        </footer>
      )}
    </section>
  );
}
