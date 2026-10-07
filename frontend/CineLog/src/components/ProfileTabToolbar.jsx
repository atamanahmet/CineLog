/** Desktop controls row; wraps when children do not fit. */
export const PROFILE_TAB_TOOLBAR_DESKTOP_CLASS =
  "hidden min-w-0 flex-wrap items-center justify-end gap-3 md:flex";

/**
 * Recommendation and Find similar controls. Stacks below md; wrap from md up.
 */
export default function ProfileTabToolbar({
  mediaToggle,
  sortSelect,
  trailingAction,
}) {
  return (
    <>
      <div className="w-full min-w-0 md:hidden">{mediaToggle}</div>
      <div className="flex w-full min-w-0 items-center gap-3 md:hidden">
        <div className="min-w-0 flex-1">{sortSelect}</div>
        {trailingAction}
      </div>
      <div className={PROFILE_TAB_TOOLBAR_DESKTOP_CLASS}>
        {mediaToggle}
        {sortSelect}
        {trailingAction}
      </div>
    </>
  );
}
