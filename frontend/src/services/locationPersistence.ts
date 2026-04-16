import type { LocationOption } from "../types/shell.types";

export function getLocationStorageKey(tenantId: string, userId: string): string {
  return `shell.active_location.${tenantId}.${userId}`;
}

export function loadPersistedLocationId(tenantId: string, userId: string): string | null {
  return window.localStorage.getItem(getLocationStorageKey(tenantId, userId));
}

export function persistLocationId(tenantId: string, userId: string, locationId: string): void {
  window.localStorage.setItem(getLocationStorageKey(tenantId, userId), locationId);
}

export function resolveActiveLocation(
  tenantId: string,
  userId: string,
  availableLocations: LocationOption[],
): LocationOption | null {
  if (availableLocations.length === 0) {
    return null;
  }

  const persisted = loadPersistedLocationId(tenantId, userId);
  if (!persisted) {
    return availableLocations[0];
  }

  const matched = availableLocations.find((location) => location.location_id === persisted);
  return matched ?? availableLocations[0];
}
