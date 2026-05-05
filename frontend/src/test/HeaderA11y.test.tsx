/**
 * T034 [US2] Header accessibility unit tests.
 *
 * Verifies WCAG 2.1 AA compliance for the Header organism and its molecules:
 *   (a) LocationSelector has role="combobox" and aria-label
 *   (b) dropdown options are role="option" elements
 *   (d) Header root has role="banner"
 *   (e) keyboard: Enter/Space on location trigger opens dropdown
 *   (f) keyboard: Escape closes the dropdown
 *
 * TDD: These tests FAIL until T035 (LocationSelector) and T037 (Header organism)
 * are implemented.
 * Refs: NFR-003, AC-A11Y
 */

import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Header } from "../components/organisms/Header";
import type { HeaderContextData, LocationOption } from "../types/shell.types";
import { renderWithShellProviders } from "./renderWithShellProviders";

const LOCATIONS: LocationOption[] = [
  { location_id: "loc-001", location_name: "UBS Central", location_type: "UBS" },
  { location_id: "loc-002", location_name: "UPA Norte", location_type: "UPA" },
];

const HEADER_CONTEXT: HeaderContextData = {
  tenant_name: "Prefeitura SP",
  tenant_id: "tenant-sp-001",
  available_locations: LOCATIONS,
  active_location_id: "loc-001",
  active_location_name: "UBS Central",
  practitioner_id: "pract-42",
  practitioner_name: "Dr. Ana Costa",
  practitioner_role: "Médico",
};

describe("Header accessibility", () => {
  describe("(d) Header root landmark", () => {
    it('has role="banner" on the root element', () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      expect(screen.getByRole("banner")).toBeInTheDocument();
    });
  });

  describe("(a) LocationSelector ARIA roles", () => {
    it('has role="combobox" on the location selector trigger', () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      expect(screen.getByRole("combobox", { name: /localização|location/i })).toBeInTheDocument();
    });

    it("has aria-label on the location selector", () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      const combobox = screen.getByRole("combobox", { name: /localização|location/i });
      expect(combobox).toHaveAttribute("aria-label");
    });

    it("has aria-expanded=false when dropdown is closed", () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      const combobox = screen.getByRole("combobox", { name: /localização|location/i });
      expect(combobox).toHaveAttribute("aria-expanded", "false");
    });

    it("has aria-expanded=true after opening", async () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      const combobox = screen.getByRole("combobox", { name: /localização|location/i });
      fireEvent.click(combobox);
      await waitFor(() => {
        expect(combobox).toHaveAttribute("aria-expanded", "true");
      });
    });
  });

  describe("(b) dropdown option roles", () => {
    it('lists options with role="option"', async () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      fireEvent.click(screen.getByRole("combobox", { name: /localização|location/i }));
      await waitFor(() => {
        const options = screen.getAllByRole("option");
        expect(options.length).toBeGreaterThanOrEqual(2);
      });
    });

    it("marks the active location as aria-selected=true", async () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      fireEvent.click(screen.getByRole("combobox", { name: /localização|location/i }));
      await waitFor(() => {
        const activeOption = screen.getByRole("option", { name: "UBS Central" });
        expect(activeOption).toHaveAttribute("aria-selected", "true");
      });
    });
  });

  describe("(e) keyboard: Enter/Space opens dropdown", () => {
    it("opens the dropdown on Enter key", async () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      const combobox = screen.getByRole("combobox", { name: /localização|location/i });
      combobox.focus();
      fireEvent.keyDown(combobox, { key: "Enter", code: "Enter" });
      await waitFor(() => {
        expect(combobox).toHaveAttribute("aria-expanded", "true");
      });
    });

    it("opens the dropdown on Space key", async () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      const combobox = screen.getByRole("combobox", { name: /localização|location/i });
      combobox.focus();
      fireEvent.keyDown(combobox, { key: " ", code: "Space" });
      await waitFor(() => {
        expect(combobox).toHaveAttribute("aria-expanded", "true");
      });
    });
  });

  describe("(f) keyboard: Escape closes dropdown", () => {
    it("closes the dropdown on Escape key after opening", async () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      const combobox = screen.getByRole("combobox", { name: /localização|location/i });
      fireEvent.click(combobox);
      await waitFor(() => expect(combobox).toHaveAttribute("aria-expanded", "true"));
      fireEvent.keyDown(combobox, { key: "Escape", code: "Escape" });
      await waitFor(() => {
        expect(combobox).toHaveAttribute("aria-expanded", "false");
      });
    });
  });
});
