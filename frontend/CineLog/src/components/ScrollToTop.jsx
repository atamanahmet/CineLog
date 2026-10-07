import { useLayoutEffect } from "react";
import { useLocation } from "react-router";

/**
 * Puts the window at the top when the path changes.
 */
export default function ScrollToTop() {
  const { pathname } = useLocation();

  useLayoutEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return null;
}
