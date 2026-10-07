import { useState } from "react";
import { Link, useLocation } from "react-router";
import movieLogo from "/movie.png";
import { cn } from "@/lib/utils";
import { useAuthStore } from "../stores/authStore";
import { useAuthModalStore } from "../stores/authModalStore";
import { PRIMARY_NAV_ITEMS, navItemHref } from "../lib/navItems";
import { getVisibleNavItems, isNavItemActive } from "../lib/navActive";
import DiscoverMenu from "./DiscoverMenu";
import MobileNav from "./MobileNav";
import NavSearch from "./NavSearch";
import ThemeToggle from "./ThemeToggle";
import UserMenu from "./UserMenu";

const NAV_LINK_CLASS =
  "inline-flex items-center border-b-2 px-1 py-1 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring";

/**
 * App top bar: discover menu, primary links, search, theme, account.
 */
export default function Navbar({ showHeader }) {
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const openModal = useAuthModalStore((s) => s.openModal);
  const [mobileOpen, setMobileOpen] = useState(false);

  const primary = getVisibleNavItems(PRIMARY_NAV_ITEMS, Boolean(user));

  const linkClass = (item) => {
    const active = isNavItemActive(item, location);
    return cn(
      NAV_LINK_CLASS,
      active
        ? "border-primary text-foreground"
        : "border-transparent text-foreground/70 hover:text-foreground",
    );
  };

  return (
    <nav
      className={`fixed top-0 right-0 left-0 z-50 h-[var(--header-height)] border-b border-border bg-card/85 text-foreground shadow-sm backdrop-blur-md transition-[transform,background-color] duration-300 ${
        showHeader ? "translate-y-0" : "-translate-y-full"
      }`}
    >
      <div className="page-container flex h-full min-w-0 items-center justify-between gap-2">
        <div className="flex min-w-0 items-center gap-2 lg:gap-4">
          <MobileNav
            open={mobileOpen}
            onOpenChange={setMobileOpen}
            isAuthenticated={Boolean(user)}
          />

          <a href="/" className="flex min-w-0 items-center gap-3">
            <img src={movieLogo} className="h-8 shrink-0" alt="Logo" />
            <span className="min-w-0 truncate self-center text-2xl font-semibold text-foreground">
              Cinelog
            </span>
          </a>

          <div className="hidden items-center gap-3 lg:flex">
            <DiscoverMenu />
            {primary.map((item) => (
              <Link
                key={item.id}
                to={navItemHref(item)}
                aria-current={
                  isNavItemActive(item, location) ? "page" : undefined
                }
                className={linkClass(item)}
              >
                {item.label}
              </Link>
            ))}
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-2 lg:gap-3">
          <div className="hidden items-center gap-3 lg:flex">
            <ThemeToggle />
            <NavSearch className="relative w-48 xl:w-56" />
          </div>

          <div className="ml-1 flex shrink-0 items-center gap-3">
            {user ? (
              <UserMenu />
            ) : (
              <>
                <button
                  type="button"
                  className="top-buttons me-1 hidden cursor-pointer rounded-lg bg-primary px-4 py-2 text-sm text-primary-foreground lg:inline-block"
                  onClick={() => openModal("register")}
                >
                  Register
                </button>
                <button
                  type="button"
                  className="top-buttons me-1 cursor-pointer rounded-lg bg-primary px-4 py-2 text-sm text-primary-foreground"
                  onClick={() => openModal("login")}
                >
                  Login
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
