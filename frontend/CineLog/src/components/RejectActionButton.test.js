import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { TooltipProvider } from "@/components/ui/tooltip";
import RejectActionButton from "./RejectActionButton";

/**
 * Walk a React element tree for a button with the given aria-label.
 */
function findButtonByLabel(node, label) {
  if (node == null || typeof node !== "object") {
    return null;
  }
  if (node.props?.["aria-label"] === label) {
    return node;
  }
  const children = node.props?.children;
  if (Array.isArray(children)) {
    for (const child of children) {
      const hit = findButtonByLabel(child, label);
      if (hit) {
        return hit;
      }
    }
  } else if (children) {
    return findButtonByLabel(children, label);
  }
  return null;
}

describe("RejectActionButton", () => {
  it("renders the Not interested aria-label when inactive", () => {
    const html = renderToStaticMarkup(
      createElement(
        TooltipProvider,
        null,
        createElement(RejectActionButton, { onReject: () => {} }),
      ),
    );
    expect(html).toContain('aria-label="Not interested"');
  });

  it("renders Remove from Not interested when active", () => {
    const html = renderToStaticMarkup(
      createElement(
        TooltipProvider,
        null,
        createElement(RejectActionButton, {
          onReject: () => {},
          active: true,
        }),
      ),
    );
    expect(html).toContain('aria-label="Remove from Not interested"');
    expect(html).not.toContain('aria-label="Not interested"');
  });

  it("calls its handler when the button onClick runs", () => {
    const onReject = vi.fn();
    const tree = RejectActionButton({ onReject });
    const button = findButtonByLabel(tree, "Not interested");
    expect(button).toBeTruthy();
    button.props.onClick({ stopPropagation: () => {} });
    expect(onReject).toHaveBeenCalledTimes(1);
  });
});
