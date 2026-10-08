import { useState } from "react";
import { useNavigate } from "react-router";
import { useFiltersStore } from "../stores/filtersStore";

/**
 * Shared navbar search form (desktop bar and mobile Sheet).
 */
export default function NavSearch({ className, onSubmitSuccess }) {
  const navigate = useNavigate();
  const search = useFiltersStore((s) => s.search);
  const [searchQuery, setSearchQuery] = useState("");

  return (
    <div className={className ?? "relative"}>
      <div className="pointer-events-none absolute inset-y-0 start-0 flex items-center ps-3">
        <svg
          className="h-4 w-4 text-muted-foreground"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 20 20"
          aria-hidden="true"
        >
          <path
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z"
          />
        </svg>
      </div>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const term = search(searchQuery);
          if (term) {
            navigate("/search");
            onSubmitSuccess?.();
          }
        }}
      >
        <input
          type="text"
          className="block w-full rounded-lg border bg-secondary p-2 ps-10 text-sm text-foreground focus:border-primary focus:ring-primary dark:bg-secondary dark:text-foreground dark:placeholder:text-muted-foreground"
          placeholder="Search..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </form>
    </div>
  );
}
