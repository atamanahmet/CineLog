import { useNavigate } from "react-router";
import { useQueryClient } from "@tanstack/react-query";
import { LogOut, Settings, User } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { clearSessionCaches } from "../lib/clearSessionCaches";
import { useAuthStore } from "../stores/authStore";

/**
 * Avatar button in the navbar that opens the account dropdown.
 */
export default function UserMenu() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const profilePhotoUrl = useAuthStore((s) => s.profilePhotoUrl);
  const logout = useAuthStore((s) => s.logout);

  const initial = user?.charAt(0)?.toUpperCase();

  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          aria-label="Open account menu"
          className="rounded-full outline-none ring-2 ring-border transition hover:ring-primary focus-visible:ring-ring data-[state=open]:ring-primary"
        >
          <Avatar className="size-10">
            <AvatarImage src={profilePhotoUrl || undefined} alt={user} />
            <AvatarFallback className="bg-secondary text-sm font-semibold text-secondary-foreground">
              {initial}
            </AvatarFallback>
          </Avatar>
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-64">
        <div className="flex items-center gap-3 px-2.5 py-2.5">
          <Avatar className="size-10">
            <AvatarImage src={profilePhotoUrl || undefined} alt={user} />
            <AvatarFallback className="bg-secondary text-sm font-semibold text-secondary-foreground">
              {initial}
            </AvatarFallback>
          </Avatar>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold">{user}</p>
            <p className="text-xs text-muted-foreground">Signed in</p>
          </div>
        </div>

        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={() => navigate("/profile")}>
          <User />
          Profile
        </DropdownMenuItem>
        <DropdownMenuItem onSelect={() => navigate("/settings")}>
          <Settings />
          Settings
        </DropdownMenuItem>
        <DropdownMenuItem
          onSelect={() => {
            clearSessionCaches(queryClient);
            void logout().then(() => navigate("/"));
          }}
          className="text-destructive focus:bg-destructive/10 focus:text-destructive [&_svg]:text-destructive"
        >
          <LogOut />
          Log out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
