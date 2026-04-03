import React from "react";
import { X } from "lucide-react";

const VideoModal = ({ isOpen, onClose, videoUrl, title }) => {
  if (!isOpen) return null;

  const getYouTubeVideoId = (url) => {
    if (!url) return null;

    const regExp =
      /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
    const match = url.match(regExp);
    return match && match[2].length === 11 ? match[2] : null;
  };

  const videoId = getYouTubeVideoId(videoUrl);
  const embedUrl = videoId
    ? `https://www.youtube.com/embed/${videoId}?autoplay=1&rel=0`
    : videoUrl;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div
        className="absolute inset-0 bg-opacity-40 backdrop-blur-sm"
        onClick={onClose}
      ></div>

      <div className="relative bg-amber-950 rounded-lg shadow-2xl max-w-5xl w-full mx-4 md:mx-8">
        <button
          onClick={onClose}
          className="absolute -top-4 -right-4 bg-white hover:bg-gray-100 text-gray-800 rounded-full p-2 shadow-lg z-10 transition-colors duration-200"
        >
          <X size={20} />
        </button>

        {title && (
          <div className="px-6 py-4 border-b border-amber-900">
            <h3 className="text-white text-lg font-semibold">
              {title} - Trailer
            </h3>
          </div>
        )}

        <div className="relative w-full" style={{ paddingBottom: "56.25%" }}>
          <iframe
            src={embedUrl}
            className="absolute top-0 left-0 w-full h-full rounded-b-lg"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
            title={`${title} Trailer`}
          ></iframe>
        </div>
      </div>
    </div>
  );
};

export default VideoModal;
