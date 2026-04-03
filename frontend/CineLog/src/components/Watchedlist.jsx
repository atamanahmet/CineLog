import { useUser } from "../context/UserContext";

export default function Watchedlist() {
  const { user, watchedlist } = useUser();
  return (
    <>
      {Array.from(watchedlist).map((item) => (
        <div key={item.id} className="relative mb-4">
          <button
            className="absolute h-7 w-7 text-amber-100 bg-amber-200 rounded left-1 z-0 addButton"
            onClick={() => {
              handleWatchedList(item.id, "del");
            }}
          >
            <span className="text-amber-100 remove absolute">-</span>
          </button>
          <div
            onClick={() => onCardClick(item)}
            className="z-1 relative text-left"
          >
            <Card
              key={item.id}
              id={item.id}
              original_title={item.original_title}
              overview={item.overview}
              poster_path={item.poster_path}
              backdrop_path={item.backdrop_path}
              title={item.title}
              vote_average={item.vote_average.toFixed(1)}
              original_language={item.original_language}
              release_date={item.release_date}
            />
          </div>
        </div>
      ))}
    </>
  );
}
