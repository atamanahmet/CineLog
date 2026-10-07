import Card from "./Card";
import ListActionButton from "./ListActionButton";
import { useAuthStore } from "../stores/authStore";
import { useListsStore } from "../stores/listsStore";

export default function MoviePlate({ onCardClick }) {
  return (
    <>
      <CardList onCardClick={onCardClick} />
    </>
  );
}

function CardList({ onCardClick }) {
  const user = useAuthStore((s) => s.user);
  const watchlist = useListsStore((s) => s.watchlist);

  return (
    <>
      {Array.from(watchlist).map((item) => (
        <div key={item.id} className="relative mb-4">
          {user && (
            <div className="absolute top-2 right-2 z-10 flex flex-col gap-1">
              <ListActionButton item={item} listType="watchlist" />
              <ListActionButton item={item} listType="watched" />
              <ListActionButton item={item} listType="loved" />
            </div>
          )}
          <div onClick={() => onCardClick(item)} className="relative z-0">
            <Card item={item} />
          </div>
        </div>
      ))}
    </>
  );
}
