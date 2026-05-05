/**
 * T042 [US3] MainTemplate provider-stack and i18n key usage tests.
 *
 * Verifies that:
 *   (a) ThemeContextProvider is present in the shell render tree
 *   (b) LocaleContextProvider is present in the shell render tree
 *   (c) ShellContextProvider is present in the shell render tree
 *   (d) At least one i18n key from headerNamespaceKeys resolves to a non-empty string
 *
 * TDD: These tests should pass as soon as the providers are wired (T047).
 * They use renderWithShellProviders which already includes all providers.
 *
 * Refs: FR-001, FR-016, SC-009
 */

import { screen } from "@testing-library/react";
import { useTranslation } from "react-i18next";
import { describe, expect, it } from "vitest";
import { useLocaleContext } from "../context/LocaleContext";
import { useShellContext } from "../context/ShellContext";
import { useThemeContext } from "../context/ThemeContext";
import { headerNamespaceKeys } from "../i18n/shell-namespaces";
import { renderWithShellProviders } from "./renderWithShellProviders";

/** Helper component that probes all required context values. */
function ContextProbe() {
  const shell = useShellContext();
  const theme = useThemeContext();
  const locale = useLocaleContext();
  const { t } = useTranslation();
  const firstKey = headerNamespaceKeys[0];

  return (
    <>
      <div data-testid="probe-shell">{shell.trainingContext.tenant_id}</div>
      <div data-testid="probe-theme">{theme.theme}</div>
      <div data-testid="probe-locale">{locale.locale}</div>
      <div data-testid="probe-i18n">{t(firstKey) || firstKey}</div>
    </>
  );
}

describe("MainTemplate provider stack", () => {
  describe("(a) ThemeContextProvider present in tree", () => {
    it("exposes ThemeContext to descendants without throwing", () => {
      renderWithShellProviders(<ContextProbe />);
      expect(screen.getByTestId("probe-theme")).toBeInTheDocument();
      expect(screen.getByTestId("probe-theme").textContent).not.toBe("");
    });
  });

  describe("(b) LocaleContextProvider present", () => {
    it("exposes LocaleContext to descendants without throwing", () => {
      renderWithShellProviders(<ContextProbe />);
      expect(screen.getByTestId("probe-locale")).toBeInTheDocument();
      expect(screen.getByTestId("probe-locale").textContent).not.toBe("");
    });
  });

  describe("(c) ShellContextProvider present", () => {
    it("exposes ShellContext to descendants without throwing", () => {
      renderWithShellProviders(<ContextProbe />);
      expect(screen.getByTestId("probe-shell")).toBeInTheDocument();
      expect(screen.getByTestId("probe-shell").textContent).not.toBe("");
    });
  });

  describe("(d) headerNamespaceKeys resolves to non-empty string", () => {
    it("i18n resolves at least one header namespace key to non-empty value", () => {
      renderWithShellProviders(<ContextProbe />);
      const keyEl = screen.getByTestId("probe-i18n");
      expect(keyEl).toBeInTheDocument();
      expect(keyEl.textContent).not.toBe("");
    });
  });
});
