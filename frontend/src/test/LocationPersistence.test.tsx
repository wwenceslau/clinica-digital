/**
 * T032 [US2] locationPersistence.ts unit tests.
 *
 * Verifies the behavior of resolveActiveLocation and persistLocationId:
 *   (a) returns first location when nothing is persisted
 *   (b) returns the persisted location when it matches an available location
 *   (c) falls back to the first location when persisted ID is not in the list
 *   (d) returns null when available locations list is empty
 *   (e) getLocationStorageKey produces a tenant-scoped and user-scoped key
 *   (f) persistLocationId writes to localStorage and loadPersistedLocationId reads it back
 *
 * These tests MUST pass immediately (service already exists at T012).
 * Refs: FR-006, SC-004
 */

import { beforeEach, describe, expect, it } from "vitest";
import type { LocationOption } from "../types/shell.types";
import {
  getLocationStorageKey,
  loadPersistedLocationId,
  persistLocationId,
  resolveActiveLocation,
} from "../services/locationPersistence";

const TENANT_ID = "tenant-sp-001";
const USER_ID = "user-42";

const LOCATIONS: LocationOption[] = [
  { location_id: "loc-001", location_name: "UBS Central", location_type: "UBS" },
  { location_id: "loc-002", location_name: "UPA Norte", location_type: "UPA" },
  { location_id: "loc-003", location_name: "Hospital Sul", location_type: "hospital" },
];

describe("locationPersistence", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  describe("(e) getLocationStorageKey", () => {
    it("includes tenantId and userId in the key", () => {
      const key = getLocationStorageKey(TENANT_ID, USER_ID);
      expect(key).toContain(TENANT_ID);
      expect(key).toContain(USER_ID);
    });

    it("produces different keys for different tenants", () => {
      const key1 = getLocationStorageKey("tenant-a", USER_ID);
      const key2 = getLocationStorageKey("tenant-b", USER_ID);
      expect(key1).not.toBe(key2);
    });

    it("produces different keys for different users", () => {
      const key1 = getLocationStorageKey(TENANT_ID, "user-1");
      const key2 = getLocationStorageKey(TENANT_ID, "user-2");
      expect(key1).not.toBe(key2);
    });
  });

  describe("(f) persistLocationId + loadPersistedLocationId round-trip", () => {
    it("writes to localStorage and reads back the same value", () => {
      persistLocationId(TENANT_ID, USER_ID, "loc-002");
      const loaded = loadPersistedLocationId(TENANT_ID, USER_ID);
      expect(loaded).toBe("loc-002");
    });

    it("returns null when nothing has been persisted", () => {
      const loaded = loadPersistedLocationId(TENANT_ID, USER_ID);
      expect(loaded).toBeNull();
    });

    it("does not leak across tenants", () => {
      persistLocationId("tenant-a", USER_ID, "loc-001");
      const loaded = loadPersistedLocationId("tenant-b", USER_ID);
      expect(loaded).toBeNull();
    });

    it("does not leak across users", () => {
      persistLocationId(TENANT_ID, "user-1", "loc-001");
      const loaded = loadPersistedLocationId(TENANT_ID, "user-2");
      expect(loaded).toBeNull();
    });
  });

  describe("(a) returns first location when nothing is persisted", () => {
    it("returns the first available location when localStorage is empty", () => {
      const result = resolveActiveLocation(TENANT_ID, USER_ID, LOCATIONS);
      expect(result).toEqual(LOCATIONS[0]);
    });
  });

  describe("(b) returns the persisted location when it matches", () => {
    it("returns the location matching the persisted ID", () => {
      persistLocationId(TENANT_ID, USER_ID, "loc-002");
      const result = resolveActiveLocation(TENANT_ID, USER_ID, LOCATIONS);
      expect(result).toEqual({ location_id: "loc-002", location_name: "UPA Norte", location_type: "UPA" });
    });
  });

  describe("(c) falls back to first location when persisted ID is not found", () => {
    it("returns the first location when persisted ID is not in the list", () => {
      persistLocationId(TENANT_ID, USER_ID, "loc-999-gone");
      const result = resolveActiveLocation(TENANT_ID, USER_ID, LOCATIONS);
      expect(result).toEqual(LOCATIONS[0]);
    });
  });

  describe("(d) returns null for an empty locations list", () => {
    it("returns null when available locations is an empty array", () => {
      const result = resolveActiveLocation(TENANT_ID, USER_ID, []);
      expect(result).toBeNull();
    });
  });
});
