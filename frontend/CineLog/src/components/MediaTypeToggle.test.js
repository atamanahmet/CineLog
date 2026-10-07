import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import MediaTypeToggle, {
  MEDIA_TYPE_GROUP_CLASS,
  MEDIA_TYPE_SEGMENT_CLASS,
} from "./MediaTypeToggle";

describe("MediaTypeToggle", () => {
  it("keeps segment nowrap, min width, and non-shrinking group", () => {
    expect(MEDIA_TYPE_SEGMENT_CLASS).toContain("whitespace-nowrap");
    expect(MEDIA_TYPE_SEGMENT_CLASS).toContain("min-w-[5.25rem]");
    expect(MEDIA_TYPE_SEGMENT_CLASS).toContain("flex-1");
    expect(MEDIA_TYPE_GROUP_CLASS).toContain("shrink-0");
    expect(MEDIA_TYPE_GROUP_CLASS).toContain("min-w-fit");

    const html = renderToStaticMarkup(
      createElement(MediaTypeToggle, {
        allowAll: true,
        mediaType: "movie",
        onChange: () => {},
      }),
    );
    for (const cls of MEDIA_TYPE_SEGMENT_CLASS.split(/\s+/)) {
      expect(html).toContain(cls);
    }
    for (const cls of MEDIA_TYPE_GROUP_CLASS.split(/\s+/)) {
      expect(html).toContain(cls);
    }
  });
});
