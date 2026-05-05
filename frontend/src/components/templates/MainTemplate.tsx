/**
 * MainTemplate — Shell root layout template.
 *
 * Architecture:
 *   - MainTemplate (exported): sets up the shell provider stack (T047) and
 *     React.lazy boundaries (T052), then delegates to MainTemplateContent.
 *   - MainTemplateContent (private): consumes all contexts and renders the
 *     actual shell chrome (Header, Sidebar, children).
 *
 * Telemetry:
 *   - Emits "shell.render" on mount and "shell.location.changed" on location
 *     change events (T051).
 *   - Root Box carries data-trace-id / data-tenant-id attributes (T048).
 *
 * Refs: FR-001, FR-007, FR-008, FR-015, FR-016, SC-004, SC-008, SC-009
 * Tasks: T038, T039, T040, T047, T048, T050, T051, T052
 */

import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Divider from "@mui/material/Divider";
import IconButton from "@mui/material/IconButton";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import React, { Suspense, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { MainTemplateProps } from "../../types/shell.types";
import type { SidebarDomainSchema } from "../../types/domain.types";
import {
  ensureSecurityDomain,
  filterNavigationByTenant,
  getDefaultNavigationSchema,
} from "../../services/navigationSchema";
import { Header } from "../organisms/Header";
import { useAuth } from "../../context/AuthContext";
import { LocaleContextProvider, useLocaleContext } from "../../context/LocaleContext";
import { ShellContextProvider } from "../../context/ShellContext";
import { ThemeContextProvider } from "../../context/ThemeContext";
import { useTenant } from "../../context/TenantContext";
import i18n from "../../i18n/config";
import { headerNamespaceKeys } from "../../i18n/shell-namespaces"; // T050
import { emitShellTelemetry } from "../../services/observability"; // T051
import { persistLocationId, resolveActiveLocation } from "../../services/locationPersistence";

// T052 — lazy-load heavy Sidebar organism to defer its JS parsing
const Sidebar = React.lazy(() =>
  import("../organisms/Sidebar").then((m) => ({ default: m.Sidebar })),
);

type MainTemplateShellProps = MainTemplateProps & {
  navigationSchema?: SidebarDomainSchema;
  onNavigate?: (route: string) => void;
};

// ── T047: MainTemplate wraps inner content in the shell provider stack ──────

/**
 * Exported entry-point. Owns the provider hierarchy so any descendant can
 * consume ThemeContext, LocaleContext, or ShellContext without an external
 * wrapper.
 */
export function MainTemplate(props: MainTemplateShellProps) {
  const { trainingContext } = props;

  return (
    <ThemeContextProvider userId={trainingContext.user_id}>
      <LocaleContextProvider userId={trainingContext.user_id}>
        <ShellContextProvider value={{ trainingContext }}>
          <MainTemplateContent {...props} />
        </ShellContextProvider>
      </LocaleContextProvider>
    </ThemeContextProvider>
  );
}

// ── Inner content component — consumes all shell contexts ────────────────────

function MainTemplateContent({
  children,
  trainingContext,
  navigationSchema,
  onNavigate,
}: MainTemplateShellProps) {
  const tenant = useTenant();
  const { session, logout } = useAuth();
  const { locale, setLocale } = useLocaleContext();
  const navigate = useNavigate();

  const [userMenuAnchor, setUserMenuAnchor] = useState<null | HTMLElement>(null);
  const [langMenuAnchor, setLangMenuAnchor] = useState<null | HTMLElement>(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const effectiveTenantId = tenant.tenantId ?? trainingContext.tenant_id;
  const effectiveUserId = session?.practitioner?.id ?? trainingContext.user_id;
  const effectiveOrgName = tenant.organizationName || session?.tenant?.displayName || "Clínica Digital";
  const effectivePractitionerName =
    tenant.practitionerName || session?.practitioner?.displayName || session?.practitioner?.email?.split('@')[0] || trainingContext.user_id;

  // T038 — resolve active location via locationPersistence (localStorage-backed)
  const activeLocationFromContext = useMemo(() => {
    const available =
      tenant.locationId && tenant.locationName
        ? [{ location_id: tenant.locationId, location_name: tenant.locationName }]
        : [];
    return resolveActiveLocation(effectiveTenantId, effectiveUserId, available);
  }, [effectiveTenantId, effectiveUserId, tenant.locationId, tenant.locationName]);

  const [activeLocationId, setActiveLocationId] = useState<string>(
    activeLocationFromContext?.location_id ?? tenant.locationId ?? "",
  );

  // Sync activeLocationId when TenantContext resolves (async load after mount)
  useEffect(() => {
    if (activeLocationFromContext?.location_id) {
      setActiveLocationId(activeLocationFromContext.location_id);
    } else if (tenant.locationId) {
      setActiveLocationId(tenant.locationId);
    }
  }, [activeLocationFromContext, tenant.locationId]);

  const availableLocations =
    tenant.locationId && tenant.locationName
      ? [{ location_id: tenant.locationId, location_name: tenant.locationName }]
      : [];

  // T051 — emit shell.render telemetry on mount (includes T050 namespace keys)
  useEffect(() => {
    emitShellTelemetry("shell.render", trainingContext.trace_id, effectiveTenantId, {
      namespace_keys: headerNamespaceKeys.join(","),
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleLocationChange = async (locationId: string) => {
    setActiveLocationId(locationId);
    persistLocationId(effectiveTenantId, effectiveUserId, locationId);
    // T051 — emit shell.location.changed telemetry
    emitShellTelemetry("shell.location.changed", trainingContext.trace_id, effectiveTenantId, {
      locationId,
    });
    await tenant.setActiveLocation(locationId);
  };

  const schema = useMemo(() => {
    const sourceSchema = navigationSchema ?? getDefaultNavigationSchema(effectiveTenantId);
    const withSecurity = ensureSecurityDomain(sourceSchema);
    return filterNavigationByTenant(withSecurity, effectiveTenantId);
  }, [navigationSchema, effectiveTenantId]);

  const handleNavigate = (route: string) => {
    if (onNavigate) {
      onNavigate(route);
      return;
    }
    navigate(route);
  };

  const handleLocaleChange = (next: "pt-BR" | "en-US") => {
    setLocale(next);
    i18n.changeLanguage(next);
    setLangMenuAnchor(null);
  };

  const handleLogout = () => {
    setUserMenuAnchor(null);
    logout();
    navigate("/login");
  };

  const headerContext = {
    tenant_name: effectiveOrgName,
    tenant_id: effectiveTenantId,
    available_locations: availableLocations,
    active_location_id: activeLocationId,
    active_location_name:
      availableLocations.find((l) => l.location_id === activeLocationId)?.location_name ??
      tenant.locationName ??
      activeLocationId,
    practitioner_id: tenant.practitionerId ?? trainingContext.user_id,
    practitioner_name: effectivePractitionerName,
    practitioner_role: "Profissional de Saúde",
  };

  // Header right-side actions: language selector + user menu
  const headerActions = (
    <>
      <Tooltip title="Idioma / Language">
        <Button
          color="inherit"
          size="small"
          onClick={(e) => setLangMenuAnchor(e.currentTarget)}
          data-testid="shell-lang-button"
          sx={{ minWidth: 72, gap: 0.75 }}
        >
          <img
            aria-hidden="true"
            src={locale === "pt-BR"
              ? "https://flagcdn.com/w20/br.png"
              : "https://flagcdn.com/w20/us.png"}
            width={20}
            height={15}
            alt=""
            style={{ display: "block", borderRadius: 2 }}
          />
          {locale === "pt-BR" ? "PT" : "EN"}
        </Button>
      </Tooltip>
      <Menu
        anchorEl={langMenuAnchor}
        open={Boolean(langMenuAnchor)}
        onClose={() => setLangMenuAnchor(null)}
      >
        <MenuItem onClick={() => handleLocaleChange("pt-BR")} selected={locale === "pt-BR"}>
          <img
            aria-hidden="true"
            src="https://flagcdn.com/w20/br.png"
            width={20}
            height={15}
            alt=""
            style={{ marginRight: 8, display: "inline-block", borderRadius: 2, verticalAlign: "middle" }}
          />
          Português (BR)
        </MenuItem>
        <MenuItem onClick={() => handleLocaleChange("en-US")} selected={locale === "en-US"}>
          <img
            aria-hidden="true"
            src="https://flagcdn.com/w20/us.png"
            width={20}
            height={15}
            alt=""
            style={{ marginRight: 8, display: "inline-block", borderRadius: 2, verticalAlign: "middle" }}
          />
          English (US)
        </MenuItem>
      </Menu>
    </>
  );

  const sidebarUserFooter = (
    <>
      <Button
        variant="text"
        fullWidth
        onClick={(e) => setUserMenuAnchor(e.currentTarget)}
        data-testid="shell-user-button"
        aria-label="menu do usuário"
        sx={{
          justifyContent: sidebarOpen ? "flex-start" : "center",
          textTransform: "none",
          color: "text.primary",
          gap: 1,
          p: 1,
          minWidth: 0,
        }}
      >
        <Tooltip title={!sidebarOpen ? effectivePractitionerName : ""} placement="right">
          <IconButton
            component="span"
            disableRipple
            size="small"
            sx={{
              width: 32,
              height: 32,
              bgcolor: "primary.main",
              color: "primary.contrastText",
              fontSize: 13,
              fontWeight: 700,
              pointerEvents: "none",
              flexShrink: 0,
            }}
          >
            {effectivePractitionerName
              .split(' ')
              .filter(Boolean)
              .slice(0, 2)
              .map((w) => w[0].toUpperCase())
              .join('')
              || '?'}
          </IconButton>
        </Tooltip>
        {sidebarOpen && (
          <Box sx={{ minWidth: 0, textAlign: "left" }}>
            <Typography variant="body2" noWrap sx={{ fontWeight: 600 }}>
              {effectivePractitionerName}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap>
              {effectiveOrgName}
            </Typography>
          </Box>
        )}
      </Button>

      <Menu
        anchorEl={userMenuAnchor}
        open={Boolean(userMenuAnchor)}
        onClose={() => setUserMenuAnchor(null)}
      >
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="body2" fontWeight={600}>
            {effectivePractitionerName}
          </Typography>
          {tenant.organizationName && (
            <Typography variant="caption" color="text.secondary">
              {tenant.organizationName}
            </Typography>
          )}
        </Box>
        <Divider />
        <MenuItem onClick={handleLogout} data-testid="shell-logout">
          Sair
        </MenuItem>
      </Menu>
    </>
  );

  return (
    // T048 — root element carries data-trace-id + data-tenant-id for observability
    <Box
      sx={{ display: "flex", flexDirection: "column", minHeight: "100vh", bgcolor: "background.default" }}
      data-testid="shell-main-template"
      data-trace-id={trainingContext.trace_id}
      data-tenant-id={effectiveTenantId}
    >
      {/* ── Header (T039) — spans full width ────────────────── */}
      <Header
        context={headerContext}
        onLocationChange={handleLocationChange}
        actions={headerActions}
      />

      {/* ── Shell body: sidebar + main content ─────────────── */}
      <Box sx={{ display: "flex", flexGrow: 1, overflow: "hidden" }}>
        {/* Permanent left sidebar */}
        <Box
          component="aside"
          sx={{
            width: sidebarOpen ? 260 : 56,
            flexShrink: 0,
            bgcolor: "background.paper",
            borderRight: "1px solid",
            borderColor: "divider",
            display: "flex",
            flexDirection: "column",
            transition: "width 0.22s cubic-bezier(0.4,0,0.6,1)",
            overflow: "hidden",
            position: "sticky",
            top: 0,
            height: "calc(100vh - 56px)",
          }}
        >
          {/* T052 — Sidebar is lazy-loaded; Suspense fallback keeps layout stable */}
          <Suspense fallback={<Box sx={{ p: 2, height: 200 }} aria-busy="true" />}>
            <Sidebar
              schema={schema}
              onNavigate={handleNavigate}
              footer={sidebarUserFooter}
              collapsed={!sidebarOpen}
              onToggleCollapsed={() => setSidebarOpen((o) => !o)}
            />
          </Suspense>
        </Box>

        {/* Main content area */}
        <Box
          component="main"
          sx={{ flex: 1, p: 3, minWidth: 0, overflow: "auto", height: "calc(100vh - 56px)" }}
        >
          {children}
        </Box>
      </Box>
    </Box>
  );
}
