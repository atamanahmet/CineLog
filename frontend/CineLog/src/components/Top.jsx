import Card from "./Card";
import { useState, useEffect } from "react";
import { useUser } from "../context/UserContext";
import WatchlistButton from "./WatchlistButton";
import axios from "axios";
import CardPlate from "./CardPlate";
import ToggleSwitch from "./ToggleSwitch";

export default function Top({}) {
  const { user, loading, handleWatchList } = useUser();
  const [currentPage, setCurrentPage] = useState(1);

  const [result, setResult] = useState(null);
  const [adult, setAdult] = useState(false);
  const [isFetching, setIsFetching] = useState(false);

  useEffect(() => {
    let timeoutId;
    const handleScroll = () => {
      if (!isFetching) {
        const scrollY = window.scrollY;
        const visible = window.innerHeight;
        const fullHeight = document.documentElement.scrollHeight;

        if (scrollY + visible >= fullHeight * 0.9 && !isFetching) {
          console.log("Requested page, current page: " + currentPage);
          clearTimeout(timeoutId); // Clear any existing timeout
          timeoutId = setTimeout(() => {
            setCurrentPage((prev) => prev + 1); // Increment page after a short delay
          }, 100);
        }
      }
    };

    window.addEventListener("scroll", handleScroll);
    clearTimeout(timeoutId);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const fetchData = async () => {
    if (isFetching) return;
    setIsFetching(true);
    try {
      const res = await axios.get("http://localhost:8080", {
        params: {
          adult,
          page: currentPage,
          sort: "vote_average.desc",
          releaseWindow: "2025-12-31",
          compare: "popularity",
          vote_count: 1000,
        },
        withCredentials: true,
      });
      setResult((prev) => {
        if (!prev || currentPage == 1) return res.data;
        const existingIds = new Set(prev.map((item) => item.id));
        const filteredNewItems = res.data.filter((item) => {
          const releaseYear = new Date(item.release_date).getFullYear();
          return !existingIds.has(item.id) && releaseYear < 2026;
        });
        return [...prev, ...filteredNewItems];
      });
    } catch (err) {
      console.error("Backend error:", err);
    } finally {
      setIsFetching(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [currentPage, adult]);

  if (!result) {
    return <div>Loading...</div>;
  }

  function handleToogle() {
    setAdult((prev) => !prev);
  }

  return (
    <>
      <h2 className="text-center p-7  text-amber-100 font-bold text-4xl bg-red-900">
        Top movies
      </h2>

      <div className="flex flex-col  max-w-11/12 justify-center mx-auto ">
        <main className=" my-10 flex flex-row flex-wrap gap-5 justify-center discoverPage">
          <CardPlate data={result} message={"Loading.."} />
        </main>
      </div>
    </>
  );
}
