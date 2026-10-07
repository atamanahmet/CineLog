import { useRef } from "react";
import { Camera, ImageUp, UserRound } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "../../../stores/authStore";
import useProfilePhotoUpload from "../hooks/useProfilePhotoUpload";
import SettingsSection from "./SettingsSection";
import StatusMessage from "./StatusMessage";

export default function ProfilePhotoSection() {
  const user = useAuthStore((s) => s.user);
  const profilePhotoUrl = useAuthStore((s) => s.profilePhotoUrl);
  const inputRef = useRef(null);
  const { file, previewUrl, status, isUploading, selectFile, cancel, upload, accept } =
    useProfilePhotoUpload();

  const openPicker = () => inputRef.current?.click();

  return (
    <SettingsSection
      id="profile"
      icon={UserRound}
      title="Profile"
      description="Your photo appears in the navbar and on your lists."
      footer={
        file && (
          <>
            <Button variant="ghost" onClick={cancel} disabled={isUploading}>
              Cancel
            </Button>
            <Button onClick={upload} disabled={isUploading}>
              {isUploading ? "Uploading..." : "Save photo"}
            </Button>
          </>
        )
      }
    >
      <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
        <button
          type="button"
          onClick={openPicker}
          aria-label="Choose profile photo"
          className="group relative size-24 shrink-0 rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <Avatar className="size-24 border-2 border-border">
            <AvatarImage src={previewUrl || profilePhotoUrl || undefined} alt={user} />
            <AvatarFallback className="bg-secondary text-3xl font-semibold text-secondary-foreground">
              {user?.charAt(0)?.toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <span className="absolute inset-0 flex items-center justify-center rounded-full bg-black/50 text-white opacity-0 transition-opacity group-hover:opacity-100">
            <Camera className="size-6" />
          </span>
        </button>

        <div className="space-y-2">
          <p className="text-lg font-semibold text-card-foreground">{user}</p>
          <p className="text-sm text-muted-foreground">JPG or PNG, up to 5 MB.</p>
          <Button variant="outline" size="sm" onClick={openPicker} disabled={isUploading}>
            <ImageUp />
            {file ? "Choose another" : "Upload new photo"}
          </Button>
          <input
            ref={inputRef}
            type="file"
            accept={accept}
            className="hidden"
            onChange={(e) => {
              selectFile(e.target.files?.[0]);
              e.target.value = "";
            }}
          />
          <StatusMessage status={status} />
        </div>
      </div>
    </SettingsSection>
  );
}
