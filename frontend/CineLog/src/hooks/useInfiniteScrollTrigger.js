import { useEffect } from "react";

/**
 * Debounce used by DiscoverPage / TopPage scroll load-more.
 */
const SCROLL_DEBOUNCE_MS = 100;

/**
 * Fire onLoadMore when the viewport crosses 75% of document height.
 */
export default function useInfiniteScrollTrigger(onLoadMore, isFetching) {
  useEffect(() => {
    let timeoutId;

    const handleScroll = () => {
      if (isFetching) {
        return;
      }
      const scrollY = window.scrollY;
      const visible = window.innerHeight;
      const fullHeight = document.documentElement.scrollHeight;

      if (scrollY + visible >= fullHeight * 0.75) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
          onLoadMore();
        }, SCROLL_DEBOUNCE_MS);
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => {
      clearTimeout(timeoutId);
      window.removeEventListener("scroll", handleScroll);
    };
  }, [onLoadMore, isFetching]);
}
