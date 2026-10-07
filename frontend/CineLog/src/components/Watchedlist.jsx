import Card from "./Card";
import ListActionButton from "./ListActionButton";
import { useAuthStore } from "../stores/authStore";
import { useListsStore } from "../stores/listsStore";

export default function Watchedlist() {
  const user = useAuthStore((s) => s.user);
  const watchedlist = useListsStore((s) => s.watchedlist);

  return (
    <>
      {Array.from(watchedlist).map((item) => (
        <div key={item.id} className="relative mb-4">
          {user && (
            <div className="absolute top-2 right-2 z-10 flex flex-col gap-1">
              <ListActionButton item={item} listType="watchlist" />
              <ListActionButton item={item} listType="watched" />
              <ListActionButton item={item} listType="loved" />
            </div>
          )}
          <div className="z-1 relative text-left">
            <Card item={item} />
          </div>
        </div>
      ))}
    </>
  );
}
