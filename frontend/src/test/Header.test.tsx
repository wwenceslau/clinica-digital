/**
 * T031 [US2] Header context rendering unit tests.
 *
 * Verifies that the Header organism correctly renders:
 *   (0) application title
 *   (a) tenant name from context
 *   (b) active location name via LocationSelector
 *   (d) location selector opens a dropdown with available locations
 *   (e) selecting a location triggers the onLocationChange callback
 *   (f) only the current tenant's data is rendered (no cross-tenant leakage)
 *
 * TDD: These tests must be committed FAILING before Header.tsx is created.
 * After T037 (Header organism implemented), all tests must pass.
 *
 * Refs: FR-005, SC-003
 */

import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Header } from "../components/organisms/Header";
import type { HeaderContextData, LocationOption } from "../types/shell.types";
import { renderWithShellProviders } from "./renderWithShellProviders";

const LOCATIONS: LocationOption[] = [
  { location_id: "loc-001", location_name: "UBS Central", location_type: "UBS" },
  { location_id: "loc-002", location_name: "UPA Norte", location_type: "UPA" },
  { location_id: "loc-003", location_name: "Hospital Sul", location_type: "hospital" },
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

const OTHER_TENANT_CONTEXT: HeaderContextData = {
  tenant_name: "Prefeitura RJ",
  tenant_id: "tenant-rj-999",
  available_locations: [{ location_id: "loc-rj-01", location_name: "UBS Rio" }],
  active_location_id: "loc-rj-01",
  active_location_name: "UBS Rio",
  practitioner_id: "pract-99",
  practitioner_name: "Dr. Carlos Lima",
  practitioner_role: "Enfermeiro",
};

describe("Header", () => {
  describe("(0) application title rendering", () => {
    it("renders the static application title", () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      expect(screen.getByTestId("header-app-title")).toHaveTextContent("Clinica Digital");
    });
  });

  describe("(a) tenant name rendering", () => {
    it("renders the tenant_name from context", () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      expect(screen.getByTestId("header-tenant-name")).toHaveTextContent("Prefeitura SP");
    });
  });

  describe("(b) active location name via LocationSelector", () => {
    it("renders the active_location_name in the location selector", () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      expect(screen.getByTestId("header-location-selector")).toBeInTheDocument();
      expect(screen.getByTestId("header-location-selector")).toHaveTextContent("UBS Central");
    });
  });

  describe("(d) location selector dropdown", () => {
    it("opens a dropdown listing all available locations", async () => {
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      fireEvent.click(screen.getByTestId("header-location-selector"));
      await waitFor(() => {
        expect(screen.getByRole("option", { name: "UBS Central" })).toBeInTheDocument();
        expect(screen.getByRole("option", { name: "UPA Norte" })).toBeInTheDocument();
        expect(screen.getByRole("option", { name: "Hospital Sul" })).toBeInTheDocument();
      });
    });
  });

  describe("(e) location change callback", () => {
    it("calls onLocationChange with the selected location_id when a location is selected", async () => {
      const onLocationChange = vi.fn();
      renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={onLocationChange} />
      );
      fireEvent.click(screen.getByTestId("header-location-selector"));
      await waitFor(() => screen.getByRole("option", { name: "UPA Norte" }));
      fireEvent.click(screen.getByRole("option", { name: "UPA Norte" }));
      await waitFor(() => {
        expect(onLocationChange).toHaveBeenCalledWith("loc-002");
      });
    });
  });

  describe("(f) tenant isolation safeguard", () => {
    it("does NOT render data from a different tenant when context changes", () => {
      const { rerender } = renderWithShellProviders(
        <Header context={HEADER_CONTEXT} onLocationChange={vi.fn()} />
      );
      expect(screen.getByTestId("header-tenant-name")).toHaveTextContent("Prefeitura SP");

      rerender(
        <Header context={OTHER_TENANT_CONTEXT} onLocationChange={vi.fn()} />
      );
      expect(screen.getByTestId("header-tenant-name")).toHaveTextContent("Prefeitura RJ");
      expect(screen.queryByText("Prefeitura SP")).not.toBeInTheDocument();
      expect(screen.queryByText("Dr. Ana Costa")).not.toBeInTheDocument();
    });
  });
});
