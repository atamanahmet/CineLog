import { Link, useLocation } from "react-router";
import { Menu } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { cn } from "@/lib/utils";
import useClearNavSort from "../hooks/useClearNavSort";
import {
  DISCOVER_NAV_ITEMS,
  PRIMARY_NAV_ITEMS,
  navItemHref,
} from "../lib/navItems";
import { getVisibleNavItems, isNavItemActive } from "../lib/navActive";
import NavSearch from "./NavSearch";
import ThemeToggle from "./ThemeToggle";

const LINK_CLASS =
  "flex items-center border-b-2 px-1 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring";

/**
 * Mobile hamburger Sheet: search, Discover group, auth links, theme.
 */
export default function MobileNav({ open, onOpenChange, isAuthenticated }) {
  const location = useLocation();
  const clearNavSort = useClearNavSort();
  const primary = getVisibleNavItems(PRIMARY_NAV_ITEMS, isAuthenticated);

  const linkClass = (item) => {
    const active = isNavItemActive(item, location);
    return cn(
      LINK_CLASS,
      active
        ? "border-primary text-foreground"
        : "border-transparent text-foreground/70 hover:text-foreground",
    );
  };

  const close = () => onOpenChange(false);

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          type="button"
          aria-label="Open menu"
          className="shrink-0 lg:hidden"
        >
          <Menu className="h-5 w-5" />
        </Button>
      </SheetTrigger>
      <SheetContent side="left" className="flex w-full flex-col gap-4 sm:max-w-sm">
        <SheetHeader>
          <SheetTitle className="sr-only">Navigation</SheetTitle>
        </SheetHeader>

        <NavSearch className="relative w-full" onSubmitSuccess={close} />

        <div className="flex flex-col gap-1">
          <p className="px-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Discover
          </p>
          {DISCOVER_NAV_ITEMS.map((item) => (
            <Link
              key={item.id}
              to={navItemHref(item)}
              aria-current={
                isNavItemActive(item, location) ? "page" : undefined
              }
              className={linkClass(item)}
              onClick={() => {
                clearNavSort(item.path);
                close();
              }}
            >
              {item.label}
            </Link>
          ))}
        </div>

        {primary.length > 0 ? (
          <div className="flex flex-col gap-1">
            {primary.map((item) => (
              <Link
                key={item.id}
                to={navItemHref(item)}
                aria-current={
                  isNavItemActive(item, location) ? "page" : undefined
                }
                className={linkClass(item)}
                onClick={close}
              >
                {item.label}
              </Link>
            ))}
          </div>
        ) : null}

        <Separator />
        <div className="flex items-center justify-between px-1">
          <span className="text-sm font-medium text-foreground">Theme</span>
          <ThemeToggle />
        </div>
      </SheetContent>
    </Sheet>
  );
}
