import { useState, useEffect, useRef } from "react";
import { Outlet } from "react-router";
import { useQueryClient } from "@tanstack/react-query";
import { setClearLoggedInUser } from "../api/axiosInstance";
import { clearSessionCaches } from "../lib/clearSessionCaches";
import { useAuthStore } from "../stores/authStore";
import { useListsStore } from "../stores/listsStore";
import Navbar from "./Navbar";
import LoadingRegion from "./LoadingRegion";
import { Skeleton } from "@/components/ui/skeleton";
import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";

/**
 * Full-page placeholder while auth session and lists hydrate.
 */
function SessionBootScreen() {
  return (
    <LoadingRegion
      label="Loading session"
      className="page-container flex flex-col items-center gap-6 py-20"
    >
      <Skeleton shell className="size-20 rounded-full" />
      <Skeleton className="h-7 w-48" />
      <Skeleton className="h-4 w-32" />
    </LoadingRegion>
  );
}

export default function Layout() {
  const [showHeader, setShowHeader] = useState(true);
  const [sessionReady, setSessionReady] = useState(false);
  const lastScrollY = useRef(0);
  const queryClient = useQueryClient();

  useEffect(() => {
    setClearLoggedInUser(() => {
      useAuthStore.setState({ user: null, profilePhotoUrl: null });
      clearSessionCaches(queryClient);
    });
    return () => setClearLoggedInUser(null);
  }, [queryClient]);

  useEffect(() => {
    (async () => {
      await useAuthStore.getState().fetchUser();
      if (useAuthStore.getState().user) {
        await useListsStore.getState().getWatchList();
      }
      setSessionReady(true);
    })();
  }, []);

  useEffect(() => {
    const handleScroll = () => {
      const currentY = window.scrollY;
      if (currentY > lastScrollY.current && currentY > 100) {
        setShowHeader(false);
      } else {
        setShowHeader(true);
      }
      lastScrollY.current = currentY;
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <TooltipProvider>
      <Navbar showHeader={showHeader} />
      <div className="min-h-screen bg-background pt-[var(--header-height)] text-foreground transition-colors">
        {sessionReady ? <Outlet /> : <SessionBootScreen />}
      </div>
      <Toaster />
    </TooltipProvider>
  );
}
