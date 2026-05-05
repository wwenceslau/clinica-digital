/**
 * T101 [US6] Tests for RbacPermissionGuard component.
 *
 * Verifies that:
 * - Children render when the authenticated user's profile includes the required permission.
 * - Children are hidden when the user's profile does not include the required permission.
 * - Children are hidden when the user is not authenticated.
 *
 * TDD state: RED until RbacPermissionGuard is implemented (T104).
 *
 * Refs: FR-006, iam.rbac.manage
 */
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RbacPermissionGuard } from "../components/atoms/RbacPermissionGuard";
import * as AuthContext from "../context/AuthContext";
import type { AuthContextValue } from "../context/AuthContext";
import type { SessionIssuedResponse } from "../services/iamAuthApi";

const makeMockSession = (profileType: number): SessionIssuedResponse => ({
  sessionId: 'session-t101',
  expiresAt: "2099-01-01T00:00:00Z",
  practitioner: {
    id: "user-t101",
    email: "user@t101.local",
    profileType,
    displayName: "Test User",
    accountActive: true,
    identifiers: [],
    names: [],
  },
  tenant: {
    id: "tenant-t101",
    name: "t101",
    displayName: "Tenant T101",
    cnes: "T101",
    active: true,
    accountActive: true,
    identifiers: [],
  },
});

function mockAuth(value: AuthContextValue) {
  vi.spyOn(AuthContext, "useAuth").mockReturnValue(value);
}

describe("RbacPermissionGuard", () => {
  it("renders children when profile 10 has iam.rbac.manage", () => {
    mockAuth({
      isAuthenticated: true,
      session: makeMockSession(10),
      login: vi.fn(),
      logout: vi.fn(),
    });

    render(
      <RbacPermissionGuard permission="iam.rbac.manage">
        <span>Conteudo Protegido</span>
      </RbacPermissionGuard>,
    );

    expect(screen.getByText("Conteudo Protegido")).toBeInTheDocument();
  });

  it("hides children when profile 20 does not have iam.rbac.manage", () => {
    mockAuth({
      isAuthenticated: true,
      session: makeMockSession(20),
      login: vi.fn(),
      logout: vi.fn(),
    });

    render(
      <RbacPermissionGuard permission="iam.rbac.manage">
        <span>Conteudo Protegido</span>
      </RbacPermissionGuard>,
    );

    expect(screen.queryByText("Conteudo Protegido")).not.toBeInTheDocument();
  });

  it("hides children when user is not authenticated", () => {
    mockAuth({
      isAuthenticated: false,
      session: null,
      login: vi.fn(),
      logout: vi.fn(),
    });

    render(
      <RbacPermissionGuard permission="iam.rbac.manage">
        <span>Conteudo Protegido</span>
      </RbacPermissionGuard>,
    );

    expect(screen.queryByText("Conteudo Protegido")).not.toBeInTheDocument();
  });

  it("renders children when profile 0 (super) has iam.rbac.manage", () => {
    mockAuth({
      isAuthenticated: true,
      session: makeMockSession(0),
      login: vi.fn(),
      logout: vi.fn(),
    });

    render(
      <RbacPermissionGuard permission="iam.rbac.manage">
        <span>Super Protegido</span>
      </RbacPermissionGuard>,
    );

    expect(screen.getByText("Super Protegido")).toBeInTheDocument();
  });
});
