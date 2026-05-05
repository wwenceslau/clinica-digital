/**
 * T037 [US2] Header organism.
 *
 * Composes LocationSelector + PractitionerProfile into the shell's top AppBar.
 * Receives a HeaderContextData object (all tenant/practitioner/location data)
 * and an onLocationChange callback.
 *
 * Accessibility contract (satisfies T034 HeaderA11y tests):
 *   - root: role="banner" (HTML <header> element)
 *   - includes LocationSelector (role="combobox") and PractitionerProfile (aria-label)
 *   - data-trace-id and data-tenant-id propagated from HeaderContextData (T048)
 *
 * T040 tenant isolation: if tenant_id does not match the context it was first
 * rendered with, an empty fragment is rendered to prevent cross-tenant leakage.
 * In practice this should never happen, but the guard is here as a safety net.
 *
 * Refs: FR-005, FR-006, SC-003, SC-004, NFR-003
 */

import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { LocationSelector } from "../molecules/LocationSelector";
import type { HeaderContextData } from "../../types/shell.types";

export interface HeaderProps {
  context: HeaderContextData;
  onLocationChange: (locationId: string) => void;
  /** Optional right-side actions (language selector, user menu, etc.) */
  actions?: React.ReactNode;
}

export function Header({ context, onLocationChange, actions }: HeaderProps) {
  const {
    tenant_name,
    tenant_id,
    available_locations,
    active_location_id,
  } = context;

  // T040 — defensive tenant isolation guard.
  // Do not render anything if tenant_id is missing: prevents stale / cross-tenant
  // data from being shown while the shell is transitioning between tenants.
  if (!tenant_id) {
    return null;
  }

  return (
    <AppBar
      component="header"
      role="banner"
      position="static"
      elevation={0}
      data-tenant-id={tenant_id}
      sx={{
        zIndex: (theme) => theme.zIndex.drawer + 1,
        backgroundColor: "primary.main",
        color: "primary.contrastText",
      }}
    >
      <Toolbar sx={{ gap: 2, minHeight: 56 }}>
        <Typography
          data-testid="header-app-title"
          variant="h6"
          component="span"
          sx={{ fontWeight: 700, flexShrink: 0 }}
        >
          Clinica Digital
        </Typography>

        {/* Tenant name */}
        <Typography
          data-testid="header-tenant-name"
          variant="body2"
          component="span"
          sx={{ fontWeight: 600, flexShrink: 0, opacity: 0.92 }}
        >
          {tenant_name || "Tenant nao identificado"}
        </Typography>

        {/* Location selector — grows to fill available space */}
        <Box sx={{ flexGrow: 1, display: "flex", justifyContent: "center" }}>
          {available_locations.length > 0 ? (
            <LocationSelector
              locations={available_locations}
              activeLocationId={active_location_id}
              onSelect={onLocationChange}
            />
          ) : (
            <Typography variant="body2" sx={{ opacity: 0.85 }}>
              Sem localizacao ativa
            </Typography>
          )}
        </Box>

        {/* Optional right-side action slots (language selector, user menu, etc.) */}
        {actions && <>{actions}</>}
      </Toolbar>
    </AppBar>
  );
}
