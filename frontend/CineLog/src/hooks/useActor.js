import { useEffect, useState } from "react";
import api from "../api/axiosInstance";
import { mapPersonDetail } from "../lib/personFilmography";

/**
 * True when an axios call was aborted on purpose.
 */
function isCanceled(err) {
  return err?.code === "ERR_CANCELED" || err?.name === "CanceledError";
}

/**
 * True when the person call says the person does not exist.
 */
function isNotFound(err) {
  return err?.response?.status === 404;
}

/**
 * Loads person details and credits for one TMDB person id.
 * Maps API → state via pure mapPersonDetail.
 */
export default function useActor(id) {
  const [person, setPerson] = useState(null);
  const [credits, setCredits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (id == null || id === "") {
      setPerson(null);
      setCredits([]);
      setLoading(false);
      setError(true);
      return;
    }

    const controller = new AbortController();
    let cancelled = false;
    setLoading(true);
    setError(null);

    api
      .get(`/person/${id}`, { signal: controller.signal })
      .then((response) => {
        if (cancelled) {
          return;
        }
        const mapped = mapPersonDetail(response?.data);
        if (mapped.person == null) {
          setPerson(null);
          setCredits([]);
          setError(true);
          setLoading(false);
          return;
        }
        setPerson(mapped.person);
        setCredits(mapped.credits);
        setError(null);
        setLoading(false);
      })
      .catch((err) => {
        if (cancelled || isCanceled(err)) {
          return;
        }
        setPerson(null);
        setCredits([]);
        setError(isNotFound(err) ? "not-found" : true);
        setLoading(false);
      });

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [id]);

  return { person, credits, loading, error };
}
