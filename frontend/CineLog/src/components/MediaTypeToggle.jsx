import React from "react";
import cinema from "../assets/cinema.png";
export default function MediaTypeToggle({ mediaType, onChange }) {
  return (
    <div
      role="tablist"
      aria-label="Select media type"
      className="inline-flex rounded-md bg-amber-800"
    >
      <button
        role="tab"
        aria-selected={mediaType === "movie"}
        className={`px-4 py-2 rounded-l-md focus:outline-none ${
          mediaType === "movie"
            ? "bg-amber-400 text-amber-950"
            : "text-amber-100 hover:bg-amber-700"
        }`}
        onClick={() => onChange("movie")}
      >
        Cinema
      </button>
      <button
        role="tab"
        aria-selected={mediaType === "tv"}
        className={`px-4 py-2 rounded-r-md focus:outline-none ${
          mediaType === "tv"
            ? "bg-amber-400 text-amber-950"
            : "text-amber-100 hover:bg-amber-700"
        }`}
        onClick={() => onChange("tv")}
      >
        TV
      </button>
    </div>
  );
}
