import { useEffect, useState } from "react";
import { useAuthStore } from "../../../stores/authStore";
import { getApiErrorMessage } from "../lib/apiError";

const ACCEPTED_TYPES = ["image/jpeg", "image/png"];
const MAX_BYTES = 5 * 1024 * 1024;

/**
 * Pick, preview, validate, and upload a profile photo.
 */
export default function useProfilePhotoUpload() {
  const uploadPhoto = useAuthStore((s) => s.uploadPhoto);
  const [file, setFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [status, setStatus] = useState(null);
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    if (!file) {
      setPreviewUrl(null);
      return undefined;
    }
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const selectFile = (next) => {
    setStatus(null);
    if (!next) {
      return;
    }
    if (!ACCEPTED_TYPES.includes(next.type)) {
      setStatus({ type: "error", message: "Use a JPG or PNG image." });
      return;
    }
    if (next.size > MAX_BYTES) {
      setStatus({ type: "error", message: "Image must be 5 MB or smaller." });
      return;
    }
    setFile(next);
  };

  const cancel = () => {
    setFile(null);
    setStatus(null);
  };

  const upload = async () => {
    if (!file) {
      return;
    }
    setIsUploading(true);
    setStatus(null);
    try {
      await uploadPhoto(file);
      setFile(null);
      setStatus({ type: "success", message: "Profile photo updated." });
    } catch (err) {
      setStatus({
        type: "error",
        message: getApiErrorMessage(err, "Upload failed. Try again."),
      });
    } finally {
      setIsUploading(false);
    }
  };

  return {
    file,
    previewUrl,
    status,
    isUploading,
    selectFile,
    cancel,
    upload,
    accept: ACCEPTED_TYPES.join(","),
  };
}
