import { Bookmark, CircleCheck, Heart } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { resolveMediaType } from "../utils/media";
import { isInList, useListsStore } from "../stores/listsStore";

const LIST_CONFIG = {
  watchlist: {
    storeKey: "watchlist",
    apiListType: "watchlist",
    Icon: Bookmark,
    activeLabel: "Remove from Watchlist",
    inactiveLabel: "Add to Watchlist",
  },
  watched: {
    storeKey: "watchedlist",
    apiListType: "watchedlist",
    Icon: CircleCheck,
    activeLabel: "Remove from Watched",
    inactiveLabel: "Mark as Watched",
  },
  loved: {
    storeKey: "lovedlist",
    apiListType: "lovedlist",
    Icon: Heart,
    activeLabel: "Remove from Loved",
    inactiveLabel: "Add to Loved",
  },
};

export default function ListActionButton({
  item,
  listType,
  mediaType: mediaTypeProp,
}) {
  const config = LIST_CONFIG[listType];
  const list = useListsStore((s) => s[config.storeKey]);
  const addToList = useListsStore((s) => s.addToList);
  const removeFromList = useListsStore((s) => s.removeFromList);

  const type = resolveMediaType(item, mediaTypeProp);
  const mediaId = item?.id;
  const active = isInList(list, mediaId, type);
  const { Icon, apiListType, activeLabel, inactiveLabel } = config;
  const label = active ? activeLabel : inactiveLabel;

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          type="button"
          size="icon"
          variant={active ? "default" : "outline"}
          aria-label={label}
          aria-pressed={active}
          onClick={(e) => {
            e.stopPropagation();
            if (active) {
              removeFromList(type, mediaId, apiListType);
            } else {
              addToList(type, mediaId, apiListType);
            }
          }}
        >
          <Icon className={active ? "fill-current" : undefined} />
        </Button>
      </TooltipTrigger>
      <TooltipContent side="top">{label}</TooltipContent>
    </Tooltip>
  );
}
