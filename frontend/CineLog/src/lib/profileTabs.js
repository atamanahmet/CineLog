import {
  Ban,
  Bookmark,
  CheckCircle2,
  Heart,
  Search,
  Sparkles,
} from "lucide-react";

/** Profile page tab strip values and labels. */
export const TABS = [
  { value: "Watchlist", label: "Watchlist" },
  { value: "Watchedlist", label: "Watched" },
  { value: "Loved", label: "Loved" },
  { value: "Not interested", label: "Not interested" },
  { value: "Recommendation", label: "Recommendation" },
  { value: "Find similar", label: "Find similar" },
];

/** User menu list shortcuts in the same order as TABS. */
export const LIST_ITEMS = [
  { tab: "Watchlist", label: "My Watchlist", icon: Bookmark },
  { tab: "Watchedlist", label: "Watched", icon: CheckCircle2 },
  { tab: "Loved", label: "Loved", icon: Heart },
  { tab: "Not interested", label: "Not interested", icon: Ban },
  { tab: "Recommendation", label: "Recommendations", icon: Sparkles },
  { tab: "Find similar", label: "Find similar", icon: Search },
];
