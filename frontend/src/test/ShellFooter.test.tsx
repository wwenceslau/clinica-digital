/**
 * T041 [US3] ShellFooter telemetry visibility unit tests.
 *
 * Verifies that the ShellFooter molecule:
 *   (a) renders data-trace-id attribute on root when debugMode=true
 *   (b) renders data-tenant-id attribute on root when debugMode=true
 *   (c) does NOT render visible footer when debugMode=false
 *   (d) renders visible footer when debugMode=true
 *
 * TDD: These tests must be committed FAILING before ShellFooter.tsx is created.
 * After T046 (ShellFooter molecule implemented), all tests must pass.
 *
 * Refs: FR-007, SC-004
 */

import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ShellFooter } from "../components/molecules/ShellFooter";
import { renderWithShellProviders } from "./renderWithShellProviders";

describe("ShellFooter", () => {
  describe("(a) data-trace-id attribute", () => {
    it("renders data-trace-id on root element when debugMode=true", () => {
      renderWithShellProviders(
        <ShellFooter traceId="trace-abc-123" tenantId="tenant-sp-001" debugMode={true} />
      );
      expect(screen.getByTestId("shell-footer")).toHaveAttribute("data-trace-id", "trace-abc-123");
    });
  });

  describe("(b) data-tenant-id attribute", () => {
    it("renders data-tenant-id on root element when debugMode=true", () => {
      renderWithShellProviders(
        <ShellFooter traceId="trace-abc-123" tenantId="tenant-sp-001" debugMode={true} />
      );
      expect(screen.getByTestId("shell-footer")).toHaveAttribute("data-tenant-id", "tenant-sp-001");
    });
  });

  describe("(c) footer hidden when debug_mode=false", () => {
    it("does not render a visible footer when debugMode=false", () => {
      renderWithShellProviders(
        <ShellFooter traceId="trace-abc-123" tenantId="tenant-sp-001" debugMode={false} />
      );
      expect(screen.queryByTestId("shell-footer")).not.toBeInTheDocument();
    });
  });

  describe("(d) footer visible when debug_mode=true", () => {
    it("renders a visible footer element when debugMode=true", () => {
      renderWithShellProviders(
        <ShellFooter traceId="trace-abc-123" tenantId="tenant-sp-001" debugMode={true} />
      );
      expect(screen.getByTestId("shell-footer")).toBeVisible();
    });
  });
});
