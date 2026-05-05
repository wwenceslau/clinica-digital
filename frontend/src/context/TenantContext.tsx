/**
 * T076 [US5] TenantContext — practitioner + tenant context for the Shell.
 *
 * Derives the minimum required context from the authenticated session:
 * tenantId, organizationId, organizationName, locationId, locationName,
 * practitionerId, practitionerName, profileType.
 *
 * T088 [US11] Extended to fetch GET /api/users/me/context on mount when the
 * user is authenticated, and to expose setActiveLocation for selecting a
 * location via POST /api/users/me/active-location.
 *
 * Must be rendered inside AuthProvider so it can read the session.
 * Refs: FR-008, FR-012, FR-018, FR-019
 */

import { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { useAuth } from './AuthContext';
import { getMyContext, setActiveLocation as apiSetActiveLocation } from '../services/iamAuthApi';
import type { UserContextResponse } from '../services/iamAuthApi';

const SYSTEM_TENANT_ID = '00000000-0000-0000-0000-000000000000';

export interface TenantContextValue {
  tenantId: string | null;
  organizationId: string | null;
  organizationName: string | null;
  /** Location is resolved from the active practitioner role in the session. */
  locationId: string | null;
  locationName: string | null;
  practitionerId: string | null;
  practitionerName: string | null;
  profileType: number | null;
  /** Select the active location for this session. Updates the shell header. */
  setActiveLocation: (locationId: string) => Promise<void>;
}

const TenantCtx = createContext<TenantContextValue | undefined>(undefined);

interface TenantProviderProps {
  children: ReactNode;
}

export function TenantProvider({ children }: TenantProviderProps) {
  const { session } = useAuth();
  const [remoteContext, setRemoteContext] = useState<UserContextResponse | null>(null);

  // Fetch the full user context from the backend when authenticated.
  // The backend resolves the active location, practitioner role, etc.
  useEffect(() => {
    if (!session) {
      setRemoteContext(null);
      return;
    }
    // Super-user (profile 0) sessions have no practitioner context; skip remote fetch.
    if (session.tenant.id === SYSTEM_TENANT_ID) {
      setRemoteContext(null);
      return;
    }
    let cancelled = false;
    getMyContext(session.tenant.id)
      .then((ctx) => {
        if (!cancelled) setRemoteContext(ctx);
      })
      .catch(() => {
        // Context fetch is best-effort; fall back to session-derived values.
        if (!cancelled) setRemoteContext(null);
      });
    return () => {
      cancelled = true;
    };
  }, [session]);

  const handleSetActiveLocation = async (locationId: string): Promise<void> => {
    const updated = await apiSetActiveLocation(locationId);
    setRemoteContext(updated);
  };

  // Merge: prefer remote context fields when available, fall back to session.
  const tenantId = remoteContext?.tenantId ?? session?.tenant.id ?? null;
  const organizationId = remoteContext?.organizationId ?? session?.tenant.id ?? null;
  const organizationName = remoteContext?.organizationName ?? session?.tenant.displayName ?? null;
  const locationId = remoteContext?.locationId ?? null;
  const locationName = remoteContext?.locationName ?? null;
  const practitionerId = remoteContext?.practitionerId ?? session?.practitioner.id ?? null;
  const practitionerName = remoteContext?.practitionerName ?? session?.practitioner.displayName ?? null;
  const profileType = remoteContext?.profileType ?? session?.practitioner.profileType ?? null;

  const value: TenantContextValue = {
    tenantId,
    organizationId,
    organizationName,
    locationId,
    locationName,
    practitionerId,
    practitionerName,
    profileType,
    setActiveLocation: handleSetActiveLocation,
  };

  return <TenantCtx.Provider value={value}>{children}</TenantCtx.Provider>;
}

export function useTenant(): TenantContextValue {
  const ctx = useContext(TenantCtx);
  if (!ctx) {
    throw new Error('useTenant must be used inside TenantProvider');
  }
  return ctx;
}

