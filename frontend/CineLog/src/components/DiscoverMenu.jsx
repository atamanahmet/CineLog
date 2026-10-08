import { Link, useLocation } from "react-router";
import { Check, ChevronDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";
import useClearNavSort from "../hooks/useClearNavSort";
import { DISCOVER_NAV_ITEMS, navItemHref } from "../lib/navItems";
import { isNavItemActive } from "../lib/navActive";

const TRIGGER_CLASS =
  "h-auto gap-1 rounded-none border-b-2 bg-transparent px-1 py-1 text-sm font-medium shadow-none hover:bg-transparent hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring data-[state=open]:bg-transparent data-[state=open]:text-foreground";

/**
 * Desktop Discover dropdown with real Link items.
 */
export default function DiscoverMenu() {
  const location = useLocation();
  const clearNavSort = useClearNavSort();
  const anyActive = DISCOVER_NAV_ITEMS.some((item) =>
    isNavItemActive(item, location),
  );

  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          type="button"
          className={cn(
            TRIGGER_CLASS,
            anyActive
              ? "border-primary text-foreground"
              : "border-transparent text-foreground/70 hover:text-foreground",
          )}
        >
          Discover
          <ChevronDown className="h-4 w-4 opacity-70" aria-hidden="true" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-48">
        {DISCOVER_NAV_ITEMS.map((item) => {
          const active = isNavItemActive(item, location);
          return (
            <DropdownMenuItem key={item.id} asChild>
              <Link
                to={navItemHref(item)}
                aria-current={active ? "page" : undefined}
                onClick={() => clearNavSort(item.path)}
                className="flex cursor-pointer items-center justify-between gap-2"
              >
                <span>{item.label}</span>
                {active ? (
                  <Check className="h-4 w-4 shrink-0" aria-hidden="true" />
                ) : null}
              </Link>
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
