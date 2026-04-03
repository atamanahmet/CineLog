import { useUser } from "../context/UserContext";
import emptyHeart from "../assets/emptyHeart.png";
import solidHeart from "../assets/solidHeart.png";
import openedEye from "../assets/opened-50-centered.svg";
import closedEye from "../assets/closed-50-centered.svg";

export default function ListButton({ item, style }) {
  const { handleList, lovedlistIdSet, watchedlistIdSet, watchlistIdSet } =
    useUser();

  const isInLovedlist = lovedlistIdSet.has(item.id);
  const isInWatchedlist = watchedlistIdSet.has(item.id);
  const isInWatchlist = watchlistIdSet.has(item.id);

  //   console.log(item.title + ": " + isInWatchedlist);
  return (
    <>
      <div>
        {/* Watchlist */}
        <button
          className={style.watchlist}
          onClick={() => {
            handleList(
              item.first_air_date != null ? "tv" : "movie",
              item.id,
              isInWatchlist ? "del" : "add",
              "watchlist"
            );
          }}
        >
          {isInWatchlist ? (
            <span className="text-amber-100 remove absolute ">-</span>
          ) : (
            <span className="text-amber-100 add absolute">+</span>
          )}
        </button>

        {/* Watched*/}
        <button
          className={style.watchedlist}
          onClick={() => {
            handleList(
              item.first_air_date != null ? "tv" : "movie",
              item.id,
              isInWatchedlist ? "del" : "add",
              "watchedlist"
            );
          }}
        >
          {isInWatchedlist ? (
            <span className="text-amber-100 add size-5 opacity-80">
              <img src={closedEye} alt="" className=" closedEye max-w-10/12" />
            </span>
          ) : (
            <span className="text-amber-100 add size-6 ">
              <img src={openedEye} alt="" className="closedEye max-w-10/12" />
            </span>
          )}
        </button>

        {/* Loved*/}
        <button
          className={style.lovedlist}
          onClick={() => {
            handleList(
              item.first_air_date != null ? "tv" : "movie",
              item.id,
              isInLovedlist ? "del" : "add",
              "lovedlist"
            );
          }}
        >
          {isInLovedlist ? (
            <span className="text-amber-100 add size-4 ">
              <img
                src={solidHeart}
                alt="loved"
                className=" closedEye max-w-10/12 scale-90 "
              />
            </span>
          ) : (
            <span className="text-amber-100 add size-6">
              <img
                src={emptyHeart}
                alt="not loved yet"
                className="closedEye max-w-10/12  scale-90"
              />
            </span>
          )}
        </button>
      </div>
    </>
  );
}
