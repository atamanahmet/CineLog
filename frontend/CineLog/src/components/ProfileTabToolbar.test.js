import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import ProfileTabToolbar, {
  PROFILE_TAB_TOOLBAR_DESKTOP_CLASS,
} from "./ProfileTabToolbar";

describe("ProfileTabToolbar", () => {
  it("wraps the desktop row and never forces nowrap", () => {
    expect(PROFILE_TAB_TOOLBAR_DESKTOP_CLASS).toContain("flex-wrap");
    expect(PROFILE_TAB_TOOLBAR_DESKTOP_CLASS).not.toContain("flex-nowrap");
    expect(PROFILE_TAB_TOOLBAR_DESKTOP_CLASS).not.toContain("md:flex-nowrap");

    const html = renderToStaticMarkup(
      createElement(ProfileTabToolbar, {
        mediaToggle: createElement("span", null, "toggle"),
        sortSelect: createElement("span", null, "sort"),
        trailingAction: createElement("span", null, "action"),
      }),
    );
    for (const cls of PROFILE_TAB_TOOLBAR_DESKTOP_CLASS.split(/\s+/)) {
      expect(html).toContain(cls);
    }
    expect(html).not.toContain("flex-nowrap");
  });
});
