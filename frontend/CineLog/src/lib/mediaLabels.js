const MEDIA_TYPE_LABELS = {
  movie: "Movies",
  tv: "TV Shows",
};

export function getMediaTypeLabel(mediaType) {
  return MEDIA_TYPE_LABELS[mediaType] ?? MEDIA_TYPE_LABELS.movie;
}
