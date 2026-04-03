import { useUser } from "../context/UserContext";
import openedEye from "../assets/opened-50-centered.svg";
import closedEye from "../assets/closed-50-centered.svg";

export default function WatchedlistButton({ item }) {
  const { handleList, watchedlistIdSet } = useUser();

  const isInWatchedlist = watchedlistIdSet.has(item.id);
  // console.log(watchedlistIds);
  const buttonType = "watchedlist";
  return (
    <>
      <button
        className=" h-7 w-7 text-amber-100 bg-amber-200 rounded z-0 addButton absolute ml-10"
        onClick={() => {
          handleList(item.id, isInWatchedlist ? "del" : "add", buttonType);
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
    </>
  );
}
