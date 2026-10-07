import { ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

/**
 * Age confirmation before any adult-content option turns on.
 */
export default function AgeGateDialog({ open, onConfirm, onCancel }) {
  return (
    <Dialog open={open} onOpenChange={(next) => !next && onCancel()}>
      <DialogContent className="max-w-md rounded-2xl border-border bg-card text-card-foreground">
        <DialogHeader className="items-center text-center sm:text-center">
          <span className="mb-2 flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
            <ShieldAlert className="size-6" />
          </span>
          <DialogTitle className="text-xl">Are you 18 or older?</DialogTitle>
          <DialogDescription className="text-muted-foreground">
            This setting can show titles and images meant for adults only. Confirm your age to
            continue.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="mt-2 gap-2 sm:justify-center sm:space-x-0">
          <Button variant="outline" onClick={onCancel} className="sm:min-w-32">
            No, go back
          </Button>
          <Button onClick={onConfirm} className="sm:min-w-32">
            Yes, I am 18+
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
