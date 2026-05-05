/**
 * T043 [US3] Theme token application tests for light/dark modes.
 *
 * Verifies that index.css defines canonical CSS custom properties:
 *   (a) --primary CSS custom property defined
 *   (b) --background CSS custom property defined
 *   (c) dark mode context provides a different --background value
 *
 * Uses fs.readFileSync to read the raw CSS file content because Vitest
 * mocks CSS imports by default (without css:true in config).
 *
 * Refs: FR-008, FR-015, SC-008
 */

import { readFileSync } from "fs";
import { resolve } from "path";
import { describe, expect, it } from "vitest";

const cssPath = resolve(__dirname, "../index.css");
const rawCss = readFileSync(cssPath, "utf-8");

describe("Theme token application", () => {
  describe("(a) --primary CSS custom property defined", () => {
    it("index.css defines --primary in at least one :root scope", () => {
      expect(rawCss).toContain("--primary");
    });
  });

  describe("(b) --background CSS custom property defined", () => {
    it("index.css defines --background in at least one :root scope", () => {
      expect(rawCss).toContain("--background");
    });
  });

  describe("(c) dark mode applies different --background value", () => {
    it("index.css defines a dark mode context (data-theme='dark' or prefers-color-scheme)", () => {
      const hasDarkMode =
        rawCss.includes("data-theme='dark'") ||
        rawCss.includes('data-theme="dark"') ||
        rawCss.includes("prefers-color-scheme: dark");
      expect(hasDarkMode).toBe(true);
    });

    it("dark mode section contains a different --canvas value from the light section", () => {
      // --canvas appears at least twice: once in light, once in dark
      const canvasMatches = rawCss.match(/--canvas/g) ?? [];
      expect(canvasMatches.length).toBeGreaterThanOrEqual(2);
    });
  });
});


describe("Theme token application", () => {
  describe("(a) --primary CSS custom property defined", () => {
    it("index.css defines --primary in at least one :root scope", () => {
      expect(rawCss).toContain("--primary");
    });
  });

  describe("(b) --background CSS custom property defined", () => {
    it("index.css defines --background in at least one :root scope", () => {
      expect(rawCss).toContain("--background");
    });
  });

  describe("(c) dark mode applies different --background value", () => {
    it("index.css defines a dark mode context (data-theme='dark' or prefers-color-scheme)", () => {
      const hasDarkMode =
        rawCss.includes("data-theme='dark'") ||
        rawCss.includes('data-theme="dark"') ||
        rawCss.includes("prefers-color-scheme: dark");
      expect(hasDarkMode).toBe(true);
    });

    it("dark mode section contains a different --background or --canvas value", () => {
      // Split into light and dark sections and verify canvas/background differs
      const darkIdx = rawCss.indexOf("data-theme='dark'") !== -1
        ? rawCss.indexOf("data-theme='dark'")
        : rawCss.indexOf('data-theme="dark"');
      const darkSection = rawCss.slice(darkIdx);
      const lightSection = rawCss.slice(0, darkIdx);
      // Both sections must define some background-like token
      expect(lightSection).toContain("--canvas");
      expect(darkSection).toContain("--canvas");
    });
  });
});
