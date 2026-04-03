import { useUser } from "../context/UserContext";

export default function WatchlistButton({ item }) {
  const { handleList, watchlistIdSet } = useUser();
  const isInWatchlist = watchlistIdSet.has(item.id);

  // console.log("isInWatchlist:" + item.title + " " + isInWatchlist);
  const buttonType = "watchlist";
  return (
    <>
      <button
        className="absolute h-7 w-7 text-amber-100 bg-amber-200 rounded left-1 z-0 addButton"
        onClick={() => {
          handleList(item.id, isInWatchlist ? "del" : "add", buttonType);
        }}
      >
        {isInWatchlist ? (
          <span className="text-amber-100 remove absolute ">-</span>
        ) : (
          <span className="text-amber-100 add absolute">+</span>
        )}
      </button>
    </>
  );
}
