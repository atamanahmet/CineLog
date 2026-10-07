import { useCallback, useEffect, useState } from "react";
import api from "../api/axiosInstance";

/**
 * True when an axios call was aborted on purpose.
 */
function isCanceled(err) {
  return err?.code === "ERR_CANCELED" || err?.name === "CanceledError";
}

/**
 * True when the details call says the title does not exist.
 */
function isNotFound(err) {
  return err?.response?.status === 404;
}

/**
 * Loads details, credits and trailer for one movie or TV id.
 */
export default function useDetails(mediaType, id) {
  const [details, setDetails] = useState(null);
  const [cast, setCast] = useState([]);
  const [trailerUrl, setTrailerUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadToken, setReloadToken] = useState(0);

  const retry = useCallback(() => {
    setReloadToken((value) => value + 1);
  }, []);

  useEffect(() => {
    if (mediaType !== "movie" && mediaType !== "tv") {
      setDetails(null);
      setCast([]);
      setTrailerUrl(null);
      setLoading(false);
      setError(null);
      return;
    }
    if (id == null || id === "") {
      setDetails(null);
      setCast([]);
      setTrailerUrl(null);
      setLoading(false);
      setError(true);
      return;
    }

    const controller = new AbortController();
    let cancelled = false;
    setLoading(true);
    setError(null);

    const config = { signal: controller.signal };

    Promise.allSettled([
      api.get(`/${mediaType}/${id}`, config),
      api.get(`/${mediaType}/${id}/credits`, config),
      api.get(`/${mediaType}/${id}/video`, config),
    ]).then(([detailsResult, creditsResult, videoResult]) => {
      if (cancelled) {
        return;
      }
      if (detailsResult.status === "rejected") {
        if (isCanceled(detailsResult.reason)) {
          return;
        }
        setDetails(null);
        setCast([]);
        setTrailerUrl(null);
        setError(isNotFound(detailsResult.reason) ? "not-found" : true);
        setLoading(false);
        return;
      }
      const payload = detailsResult.value?.data;
      if (payload == null) {
        setDetails(null);
        setCast([]);
        setTrailerUrl(null);
        setError(true);
        setLoading(false);
        return;
      }
      setDetails(payload);
      setCast(
        creditsResult.status === "fulfilled" &&
          Array.isArray(creditsResult.value.data)
          ? creditsResult.value.data
          : [],
      );
      setTrailerUrl(
        videoResult.status === "fulfilled" && videoResult.value?.data
          ? videoResult.value.data
          : null,
      );
      setError(null);
      setLoading(false);
    });

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [mediaType, id, reloadToken]);

  return { details, cast, trailerUrl, loading, error, retry };
}
