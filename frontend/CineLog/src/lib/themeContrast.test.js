import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

/**
 * Parse a #rrggbb hex into sRGB 0-255.
 */
function hexToRgb(hex) {
  const h = hex.replace("#", "");
  return [
    parseInt(h.slice(0, 2), 16),
    parseInt(h.slice(2, 4), 16),
    parseInt(h.slice(4, 6), 16),
  ];
}

/**
 * sRGB channel to linear light.
 */
function srgbToLinear(channel) {
  const x = channel / 255;
  return x <= 0.04045 ? x / 12.92 : Math.pow((x + 0.055) / 1.055, 2.4);
}

/**
 * Linear light to sRGB 0-255.
 */
function linearToSrgb(channel) {
  const x =
    channel <= 0.0031308
      ? 12.92 * channel
      : 1.055 * Math.pow(channel, 1 / 2.4) - 0.055;
  return Math.min(255, Math.max(0, Math.round(x * 255)));
}

/**
 * sRGB to OKLab.
 */
function rgbToOklab([r, g, b]) {
  const R = srgbToLinear(r);
  const G = srgbToLinear(g);
  const B = srgbToLinear(b);
  const l = 0.4122214708 * R + 0.5363325363 * G + 0.0514459929 * B;
  const m = 0.2119034982 * R + 0.6806995451 * G + 0.1073969566 * B;
  const s = 0.0883024619 * R + 0.2817188376 * G + 0.6299787005 * B;
  const l_ = Math.cbrt(l);
  const m_ = Math.cbrt(m);
  const s_ = Math.cbrt(s);
  return [
    0.2104542553 * l_ + 0.793617785 * m_ - 0.0040720468 * s_,
    1.9779984951 * l_ - 2.428592205 * m_ + 0.4505937099 * s_,
    0.0259040371 * l_ + 0.7827717662 * m_ - 0.808675766 * s_,
  ];
}

/**
 * OKLab to sRGB.
 */
function oklabToRgb([L, a, b]) {
  const l_ = L + 0.3963377774 * a + 0.2158037573 * b;
  const m_ = L - 0.1055613458 * a - 0.0638541728 * b;
  const s_ = L - 0.0894841775 * a - 1.291485548 * b;
  const l = l_ * l_ * l_;
  const m = m_ * m_ * m_;
  const s = s_ * s_ * s_;
  const R = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
  const G = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s;
  const B = -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s;
  return [linearToSrgb(R), linearToSrgb(G), linearToSrgb(B)];
}

/**
 * color-mix(in oklab, color percent%, other).
 */
function mixOklab(colorHex, percent, otherHex) {
  const a = rgbToOklab(hexToRgb(colorHex));
  const b = rgbToOklab(hexToRgb(otherHex));
  const t = percent / 100;
  return oklabToRgb([
    a[0] * t + b[0] * (1 - t),
    a[1] * t + b[1] * (1 - t),
    a[2] * t + b[2] * (1 - t),
  ]);
}

/**
 * WCAG relative luminance.
 */
function relativeLuminance(rgb) {
  return (
    0.2126 * srgbToLinear(rgb[0]) +
    0.7152 * srgbToLinear(rgb[1]) +
    0.0722 * srgbToLinear(rgb[2])
  );
}

/**
 * WCAG contrast ratio between two sRGB colors.
 */
function contrastRatio(a, b) {
  const L1 = relativeLuminance(a);
  const L2 = relativeLuminance(b);
  const hi = Math.max(L1, L2);
  const lo = Math.min(L1, L2);
  return (hi + 0.05) / (lo + 0.05);
}

/**
 * Read light-mode palette and mix formulas from App.css :root.
 */
function lightThemeFromAppCss() {
  const cssPath = join(dirname(fileURLToPath(import.meta.url)), "../App.css");
  const css = readFileSync(cssPath, "utf8");
  const rootMatch = css.match(/:root\s*\{([\s\S]*?)\n\}/);
  if (!rootMatch) {
    throw new Error(":root block missing in App.css");
  }
  const root = rootMatch[1];
  const palette = (name) => {
    const m = root.match(new RegExp(`--palette-${name}:\\s*(#[0-9a-fA-F]{6})`));
    if (!m) {
      throw new Error(`--palette-${name} missing`);
    }
    return m[1];
  };
  const thistle = palette("thistle");
  const taupe = palette("taupe");
  const ink = palette("ink");
  return {
    background: mixOklab(thistle, 18, "#ffffff"),
    mutedForeground: mixOklab(taupe, 60, ink),
    foreground: hexToRgb(ink),
  };
}

describe("light theme contrast", () => {
  it("keeps --muted-foreground at least 4.5:1 against --background", () => {
    const { background, mutedForeground } = lightThemeFromAppCss();
    expect(contrastRatio(mutedForeground, background)).toBeGreaterThanOrEqual(
      4.5,
    );
  });
});
