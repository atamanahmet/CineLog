import Card from "./Card";
import ListActionButton from "./ListActionButton";
import RejectActionButton from "./RejectActionButton";
import { buildDetailsPath, resolveMediaType } from "../utils/media";
import { useAuthStore } from "../stores/authStore";

/**
 * Renders catalog cards with list and reject actions.
 */
export default function CardPlate({
  data,
  mediaType,
  message,
  onReject,
  rejectActive = false,
}) {
  const user = useAuthStore((s) => s.user);

  if (data == null) {
    return (
      <>
        <div className="text-center">
          <h2>{message}</h2>
        </div>
      </>
    );
  }

  return (
    <>
      {Array.from(data).map((item) => {
        const type = resolveMediaType(item, mediaType);
        const to = buildDetailsPath(type, item.id);
        return (
          <div
            key={`${item.mediaType ?? "MOVIE"}-${item.id}`}
            className="relative h-full w-full min-w-0 max-sm:mb-0 sm:mb-8 sm:w-auto"
          >
            {user && onReject && (
              <div className="absolute top-2 left-2 z-10">
                <RejectActionButton
                  active={rejectActive}
                  onReject={() => onReject(item)}
                />
              </div>
            )}
            {user && (
              <div className="absolute top-2 right-2 z-10 flex flex-col gap-1">
                <ListActionButton item={item} listType="watchlist" mediaType={mediaType} />
                <ListActionButton item={item} listType="watched" mediaType={mediaType} />
                <ListActionButton item={item} listType="loved" mediaType={mediaType} />
              </div>
            )}
            <Card
              item={item}
              to={to}
              linkState={{ placeholder: item }}
            />
          </div>
        );
      })}
    </>
  );
}
