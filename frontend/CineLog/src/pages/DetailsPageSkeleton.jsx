import LoadingRegion from "../components/LoadingRegion";
import DetailsHero from "../components/DetailsHero";
import { Skeleton } from "@/components/ui/skeleton";
import {
  DETAILS_CAST_GRID_CLASS,
  DETAILS_CAST_SECTION_CLASS,
  DETAILS_CONTAINER_CLASS,
  DETAILS_INFO_CLASS,
  DETAILS_PAGE_CLASS,
  DETAILS_POSTER_CLASS,
  DETAILS_ROW_CLASS,
} from "./detailsLayout";

/**
 * Details loading layout. Same frames as the loaded page.
 */
export default function DetailsPageSkeleton() {
  return (
    <LoadingRegion label="Loading details" className={DETAILS_PAGE_CLASS}>
      <DetailsHero>
        <div className={DETAILS_ROW_CLASS}>
          <Skeleton
            shell
            data-slot="poster"
            className={DETAILS_POSTER_CLASS}
          />
          <div className={DETAILS_INFO_CLASS}>
            <div className="mb-4 space-y-2" data-slot="title">
              <Skeleton className="h-10 w-3/4 max-w-xl md:h-16" />
              <Skeleton className="h-6 w-1/2 max-w-sm" />
            </div>
            <div className="mb-6 flex flex-wrap gap-6" data-slot="rating">
              <Skeleton className="h-6 w-24" />
              <Skeleton className="h-6 w-36" />
              <Skeleton className="h-6 w-16" />
            </div>
            <div className="mb-6 flex flex-wrap gap-2" data-slot="chips">
              <Skeleton className="h-7 w-20 rounded-full" />
              <Skeleton className="h-7 w-28 rounded-full" />
              <Skeleton className="h-7 w-24 rounded-full" />
            </div>
            <div className="mb-8 space-y-2" data-slot="overview">
              <Skeleton className="h-8 w-36" />
              <Skeleton className="h-4 w-full max-w-3xl" />
              <Skeleton className="h-4 w-full max-w-3xl" />
              <Skeleton className="h-4 w-2/3 max-w-2xl" />
            </div>
            <div className="flex flex-wrap gap-4" data-slot="buttons">
              <Skeleton className="h-12 w-40 rounded-lg" />
            </div>
          </div>
        </div>
      </DetailsHero>
      <div className={DETAILS_CONTAINER_CLASS}>
        <div className={DETAILS_CAST_SECTION_CLASS} data-slot="cast">
          <Skeleton className="mb-6 h-8 w-24" />
          <div className={DETAILS_CAST_GRID_CLASS}>
            {Array.from({ length: 8 }, (_, index) => (
              <div key={index} className="text-center">
                <Skeleton className="mb-3 aspect-square w-full rounded-full" />
                <Skeleton className="mx-auto mb-1 h-4 w-3/4" />
                <Skeleton className="mx-auto h-3 w-1/2" />
              </div>
            ))}
          </div>
        </div>
      </div>
    </LoadingRegion>
  );
}
