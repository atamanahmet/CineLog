import { useEffect, useState } from "react";
import { Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import LoadingRegion from "../../../components/LoadingRegion";
import useRecommendationSettings from "../hooks/useRecommendationSettings";
import { getApiErrorMessage } from "../lib/apiError";
import {
  REC_MIN_SCORE,
  REC_SCOPE_OPTIONS,
  buildMaxResultsOptions,
  strictnessLabel,
} from "../lib/recommendationOptions";
import SegmentedControl from "./SegmentedControl";
import SettingsSection from "./SettingsSection";
import StatusMessage from "./StatusMessage";

export default function RecommendationSection() {
  const { settingsQuery, saveMutation } = useRecommendationSettings();
  const saved = settingsQuery.data;
  const [draft, setDraft] = useState(null);
  const [status, setStatus] = useState(null);

  useEffect(() => {
    if (saved) {
      const limit = saved.maxResultsLimit ?? saved.maxResults;
      setDraft({
        ...saved,
        maxResults: Math.min(saved.maxResults, limit),
      });
    }
  }, [saved]);

  const dirty =
    draft &&
    saved &&
    (draft.scope !== saved.scope ||
      draft.minScore !== saved.minScore ||
      draft.maxResults !== Math.min(saved.maxResults, saved.maxResultsLimit ?? saved.maxResults));

  const patch = (partial) => {
    setStatus(null);
    setDraft((d) => ({ ...d, ...partial }));
  };

  const handleSave = () => {
    saveMutation.mutate(
      {
        scope: draft.scope,
        minScore: draft.minScore,
        maxResults: draft.maxResults,
      },
      {
        onSuccess: () =>
          setStatus({ type: "success", message: "Saved. Recommendations are refreshing." }),
        onError: (err) =>
          setStatus({ type: "error", message: getApiErrorMessage(err, "Could not save settings.") }),
      },
    );
  };

  const maxResultsLimit = draft?.maxResultsLimit ?? saved?.maxResultsLimit;
  const maxResultsOptions = buildMaxResultsOptions(maxResultsLimit);

  return (
    <SettingsSection
      id="recommendations"
      icon={Sparkles}
      title="Recommendations"
      description="Tune how the engine picks titles from your loved list."
      footer={
        <>
          <StatusMessage status={status} />
          <Button variant="ghost" onClick={() => setDraft(saved)} disabled={!dirty}>
            Discard
          </Button>
          <Button onClick={handleSave} disabled={!dirty || saveMutation.isPending}>
            {saveMutation.isPending ? "Saving..." : "Save changes"}
          </Button>
        </>
      }
    >
      {settingsQuery.isError && (
        <StatusMessage status={{ type: "error", message: "Could not load settings." }} />
      )}
      {!draft && !settingsQuery.isError && (
        <LoadingRegion label="Loading settings" className="space-y-4">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-10 w-full rounded-lg" />
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-8 w-full rounded-lg" />
          <Skeleton className="h-4 w-28" />
          <Skeleton className="h-10 w-full rounded-lg" />
        </LoadingRegion>
      )}
      {draft && (
        <div className="space-y-6">
          <div className="space-y-2">
            <p className="text-sm font-medium text-card-foreground">Recommend</p>
            <SegmentedControl
              ariaLabel="Recommendation media type"
              options={REC_SCOPE_OPTIONS}
              value={draft.scope}
              onChange={(scope) => patch({ scope })}
            />
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label htmlFor="rec-strictness" className="text-sm font-medium text-card-foreground">
                Match strictness
              </label>
              <span className="rounded-md bg-secondary px-2 py-0.5 text-xs font-semibold text-secondary-foreground">
                {strictnessLabel(draft.minScore)} · {Math.round(draft.minScore * 100)}%
              </span>
            </div>
            <input
              id="rec-strictness"
              type="range"
              min={REC_MIN_SCORE.min}
              max={REC_MIN_SCORE.max}
              step={REC_MIN_SCORE.step}
              value={draft.minScore}
              onChange={(e) => patch({ minScore: Number(Number(e.target.value).toFixed(2)) })}
              className="w-full accent-primary"
            />
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>More variety</span>
              <span>Closer matches</span>
            </div>
          </div>

          <div className="space-y-2">
            <p className="text-sm font-medium text-card-foreground">Maximum results</p>
            <SegmentedControl
              ariaLabel="Maximum results"
              options={maxResultsOptions.map((n) => ({ value: n, label: String(n) }))}
              value={draft.maxResults}
              onChange={(maxResults) => patch({ maxResults })}
            />
          </div>
        </div>
      )}
    </SettingsSection>
  );
}
