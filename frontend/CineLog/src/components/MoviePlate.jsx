import Card from "./Card";
import { useState } from "react";
import { useUser } from "../context/UserContext";
import WatchlistButton from "./WatchlistButton";

export default function MoviePlate({ movies, onCardClick }) {
  const { user, handleWatchList, watchlist } = useUser();

  // if (watchlist) {
  //   return (
  //     <div className="text-center">
  //       <div>Loading...</div>
  //     </div>
  //   );
  // }

  return (
    <>
      <CardList
        result={watchlist}
        onCardClick={onCardClick}
        user={user}
        list={watchlist}
        addOrRemove={handleWatchList}
      />
    </>
  );
}

function CardList({ onCardClick, addOrRemove }) {
  const { user, watchlist } = useUser();
  function handleWatchlistToogle(id) {
    let actionType = null;
    const updateSet = new Set(watchlist);
    console.log(updateSet);
    if (updateSet.has(id)) {
      actionType = "del";
    } else {
      actionType = "add";
    }
    addOrRemove(id, actionType);
  }

  // if (!result) return <div>Loading film...</div>;
  // if (result == null)
  //   return <div className="text-center text-amber-700">Loading...</div>;

  return (
    <>
      {Array.from(watchlist).map((item) => (
        <div key={item.id} className="relative mb-4">
          {user && <WatchlistButton item={item} />}
          <div onClick={() => onCardClick(item)} className="z-1 relative">
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
