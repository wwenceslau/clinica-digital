# Data Model: The Shell Estrutural

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-04-10  
**Related Spec**: [spec.md](spec.md)

## Overview

The Shell data model defines the structural entities and their relationships that govern the authenticated area layout. All entities are client-side presentation models (no database persistence required for Shell itself); data originates from REST API responses or React Context.

## Key Entities

### 1. MainTemplate

**Purpose**: Root layout container that wraps the authenticated application area.

**Responsibilities**:
- Render structural regions: Header, Sidebar, main content area, Footer/Telemetry
- Provide application-level context providers (ThemeProvider, i18nProvider, ShellContext)
- Manage persistent state (theme, locale, lastLocation) via React Context
- Expose public API for child routes/components

**Props Contract**:
```typescript
interface MainTemplateProps {
  children: React.ReactNode;                      // Page content
  initialTheme?: 'light' | 'dark';               // Default: 'light'
  initialLocale?: 'pt-BR' | 'en-US';            // Default: 'pt-BR'
  trainingContext: {
    tenant_id: string;
    trace_id: string;
    user_id: string;
    role: string[];
  };
}
```

**Exported From**: `frontend/src/components/templates/MainTemplate.tsx`

---

### 2. SidebarDomainGroup

**Purpose**: Grouping container for navigation items organized by functional domain.

**Properties**:
- `domain_id`: Unique identifier (e.g., "administracao", "profissionais", "pacientes", etc.)
- `domain_label_key`: i18n namespace key (e.g., "sidebar.domain.administration")
- `description`: Plain-text domain description (for tooltips/help)
- `resources`: Array of `SidebarResourceItem`
- `visible`: Boolean; if false, entire group is hidden from navigation
- `disabled_reason`: String; if non-null, explains why group is disabled (permission/license)

**Domain Structure** (from Resource Table normative reference):

```typescript
interface SidebarDomainSchema {
  domains: [
    {
      domain_id: "administracao";
      domain_label_key: "sidebar.domain.administration";
      resources: [ "tenant-settings", "users", "integrations", "audit-log" ]
    },
    {
      domain_id: "profissionais";
      domain_label_key: "sidebar.domain.professionals";
      resources: [ "practitioner-registry", "credentials", "schedules" ]
    },
    {
      domain_id: "pacientes";
      domain_label_key: "sidebar.domain.patients";
      resources: [ "patient-search", "patient-records" ]
    },
    {
      domain_id: "agenda";
      domain_label_key: "sidebar.domain.scheduling";
      resources: [ "appointments", "available-slots" ]
    },
    {
      domain_id: "atendimento";
      domain_label_key: "sidebar.domain.clinical-care";
      resources: [ "clinical-notes", "prescriptions", "vital-signs" ]
    },
    {
      domain_id: "diagnostico-terapeutica";
      domain_label_key: "sidebar.domain.diagnostics-therapy";
      resources: [ "exam-orders", "lab-results", "treatment-plans" ]
    },
    {
      domain_id: "prevencao";
      domain_label_key: "sidebar.domain.prevention";
      resources: [ "health-programs", "screening", "education" ]
    },
    {
      domain_id: "financeiro-faturamento";
      domain_label_key: "sidebar.domain.billing";
      resources: [ "billing-invoices", "payments", "financial-reports" ]
    },
    {
      domain_id: "seguranca";
      domain_label_key: "sidebar.domain.security";
      resources: [ "internal-user-management", "access-profiles", "audit-trail" ]
    }
  ]
}
```

**Rendered Component**: `SidebarGroup.tsx` (molecule)

---

### 3. SidebarResourceItem

**Purpose**: Individual navigation item representing a module or feature within a domain.

**Properties**:
- `resource_id`: Unique identifier (e.g., "tenant-settings", "patient-search")
- `label_key`: i18n namespace key (e.g., "sidebar.administration.tenant-settings")
- `icon_id`: Icon identifier for rendering (MUI icon enum or SVG path)
- `route`: Navigation target (React Router path or external URL)
- `domain_id`: Parent domain identifier
- `permission_key`: Authorization key (e.g., "perm.administration:read:tenant-settings")
- `permitted`: Boolean; false if current user lacks permission
- `disabled_reason`: Explanation string (shown in tooltip if disabled)
- `metadata`: Dictionary; optional contextual data (e.g., badge count, feature flags)

**State Model**:
```typescript
interface SidebarResourceItemState {
  resource_id: string;
  label_key: string;
  icon_id: string;
  route: string;
  domain_id: string;
  permission_key: string;
  permitted: boolean;                // From backend authorization
  disabled_reason?: string;          // If !permitted
  metadata?: {
    badge_count?: number;
    new_badge?: boolean;
    feature_flag?: boolean;
  };
}
```

**Rendered Component**: `SidebarItem.tsx` (atom) + `SidebarItemButton.tsx` (molecule)

---

### 4. HeaderContext

**Purpose**: Displays operational context (tenant, active location, practitioner profile) in the application header.

**Properties**:
- `tenant_name`: Display name of the clinic/org
- `tenant_id`: System identifier (from JWT/request context)
- `available_locations`: Array of unit/branch locations the user can access
- `active_location_id`: Currently selected location ID (from localStorage + backend validation)
- `active_location_name`: Display name of selected location
- `practitioner_id`: System ID of authenticated user
- `practitioner_name`: Display name
- `practitioner_role`: Clinical role (e.g., "Médico", "Enfermeiro", "Administrativo")
- `practitioner_avatar_url`: Profile picture URL (optional; gravatar/initials fallback)

**Persistence Logic**:
- Last selected location is persisted in localStorage under key: `shell.active_location.{tenant_id}.{user_id}`
- On page reload, if localStorage value exists and is valid (location still available), restore it; otherwise default to first available location
- Location change triggers both UI update and backend confirmation API call

**State Model**:
```typescript
interface HeaderContextData {
  tenant_name: string;
  tenant_id: string;
  available_locations: LocationOption[];
  active_location_id: string;
  active_location_name: string;
  practitioner_id: string;
  practitioner_name: string;
  practitioner_role: string;
  practitioner_avatar_url?: string;
}

interface LocationOption {
  location_id: string;
  location_name: string;
  location_type?: string;                // e.g., "Clínica", "Hospital Dia"
}
```

**Rendered Components**: `Header.tsx` (organism), `LocationSelector.tsx` (molecule), `PractitionerProfile.tsx` (molecule)

---

### 5. ShellTelemetryMetadata

**Purpose**: Runtime observability metadata that traces system calls and tenant isolation context.

**Properties**:
- `trace_id`: Unique request trace identifier (UUID, propagated from backend headers)
- `tenant_id`: Active tenant identifier (for audit + diagnosis)
- `user_id`: Authenticated user identifier
- `timestamp_iso`: ISO-8601 timestamp of Shell render
- `visible`: Boolean; true if debug mode active OR user role includes 'support'
- `debug_mode`: Boolean; true if environment variable or feature flag enables diagnostics

**Visibility Rules**:
- **Production (visible=true if)**:
  - Debug mode environment flag is set, OR
  - User's role list includes 'support', OR
  - Backend header 'X-Debug-Mode' is present and truthy
- **Always Available**: Rendered as `data-trace-id` and `data-tenant-id` attributes in DOM even if footer is hidden; accessible to telemetry collectors and browser console
- **Rendering**: If visible=true, display in fixed footer; otherwise hidden (display: none)

**State Model**:
```typescript
interface ShellTelemetryMetadataState {
  trace_id: string;
  tenant_id: string;
  user_id: string;
  timestamp_iso: string;
  visible: boolean;                  // Calculated from role + debug flag
  debug_mode: boolean;
}
```

**Rendered Component**: `ShellFooter.tsx` (molecule) + DOM attributes on MainTemplate root

---

## Domain Navigation Schema

The domain structure is defined in a centralized JSON schema that drives the Sidebar rendering:

```json
{
  "version": "1.0.0",
  "tenant_id": "{tenant_id}",
  "domains": [
    {
      "domain_id": "seguranca",
      "domain_label_key": "sidebar.domain.security",
      "resources": [
        {
          "resource_id": "internal-user-management",
          "label_key": "sidebar.security.user-management",
          "icon_id": "people",
          "route": "/admin/security/users",
          "permission_key": "perm.security:read:user-management"
        },
        {
          "resource_id": "access-profiles",
          "label_key": "sidebar.security.access-profiles",
          "icon_id": "security",
          "route": "/admin/security/roles",
          "permission_key": "perm.security:read:access-profiles"
        },
        {
          "resource_id": "audit-trail",
          "label_key": "sidebar.security.audit",
          "icon_id": "history",
          "route": "/admin/security/audit",
          "permission_key": "perm.security:read:audit"
        }
      ]
    }
    // ... other domains
  ]
}
```

This schema is:
1. **Fetched on Shell load** from backend endpoint (`GET /api/shell/navigation-schema`)
2. **Cached in React Context** for reuse across child routes
3. **Invalidated** on location change or permission refresh
4. **Rendered** by `Sidebar.tsx` organism component

---

## Relationships & State Flow

```
MainTemplate (root)
  ├── ThemeProvider (theme state: 'light' | 'dark')
  ├── I18nProvider (locale state: 'pt-BR' | 'en-US')
  ├── ShellContext (tenant, location, telemetry)
  │
  ├── Header (organism)
  │   ├── HeaderBar (molecule)
  │   ├── LocationSelector (molecule)
  │   │   └── persists activeLocation to localStorage on change
  │   └── PractitionerProfile (molecule)
  │
  ├── Sidebar (organism)
  │   ├── SidebarGroup (molecule) [per domain]
  │   │   └── SidebarItem (atom/molecule) [per resource]
  │   │       └── disabled if !permitted
  │   └── (loads navigation schema from backend on mount)
  │
  ├── MainContent (children)
  │   └── (page content via React Router)
  │
  └── ShellFooter (molecule)
      └── TelemetryLabel (visible per debug mode / role)
```

---

## CSS Token Organization

All visual styling is driven by CSS custom properties defined in `globals.css` with dual-theme support:

```css
:root[data-theme="light"] {
  --shell-color-primary: #1976d2;
  --shell-color-secondary: #dc004e;
  --shell-color-text: #000;
  --shell-color-text-secondary: #666;
  --shell-spacing-unit: 8px;
  --shell-spacing-xs: calc(var(--shell-spacing-unit) * 0.5);  /* 4px */
  --shell-spacing-sm: var(--shell-spacing-unit);             /* 8px */
  --shell-spacing-md: calc(var(--shell-spacing-unit) * 2);   /* 16px */
  --shell-spacing-lg: calc(var(--shell-spacing-unit) * 3);   /* 24px */
  --shell-spacing-xl: calc(var(--shell-spacing-unit) * 4);   /* 32px */
  --shell-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto';
  --shell-font-size-body: 14px;
  --shell-font-size-heading: 16px;
  --shell-font-weight-regular: 400;
  --shell-font-weight-bold: 700;
  /* ... more tokens */
}

:root[data-theme="dark"] {
  --shell-color-primary: #90caf9;
  --shell-color-secondary: #f48fb1;
  --shell-color-text: #fff;
  --shell-color-text-secondary: #bbb;
  /* ... token values for dark theme */
}
```

Components apply tokens via MUI `sx` prop or Tailwind utilities.

---

## i18n Namespace Structure

All Shell strings use namespaced translation keys:

```typescript
// sidebar domain labels
"sidebar.domain.administration"
"sidebar.domain.professionals"
"sidebar.domain.patients"
"sidebar.domain.scheduling"
"sidebar.domain.clinical-care"
"sidebar.domain.diagnostics-therapy"
"sidebar.domain.prevention"
"sidebar.domain.billing"
"sidebar.domain.security"

// Resource items (Security domain)
"sidebar.security.user-management"
"sidebar.security.access-profiles"
"sidebar.security.audit"

// Header context
"header.tenant"
"header.location"
"header.profile"
"header.select-unit"

// Telemetry
"telemetry.trace-id"
"telemetry.tenant-id"
"telemetry.debug-mode"

// Accessibility messages
"a11y.permission-restricted"
"a11y.menu.open"
"a11y.menu.close"
```

Actual translation content (Portuguese/English strings) is provided post-implementation and managed in a separate localization config file.

---

## Success Criteria (from Spec Mapped to Model)

| Spec SC | Data Model Validation |
|---|---|
| SC-001 | Every domain + resource in schema matches Resource Table normative reference; visual in Sidebar |
| SC-002 | Navigation find-time testable via Playwright navigation click latency + user search paths |
| SC-003 | Header displays tenant, location, profile for every authenticated user; testable via DOM inspection |
| SC-004 | trace_id/tenant_id in ShellTelemetryMetadata; rendered per visibility rules; always in data-* attributes |
| SC-005 | Component folder structure enforced at code review (atoms/, molecules/, organisms/, templates/) |
| SC-006 | WCAG 2.1 AA compliance validated via disabled_reason tooltips, focus indicators, semantic ARIA |
| SC-007 | LCP ≤ 1.5s on 4G simulated; benchmarked in CI/CD Lighthouse audit |
| SC-008 | CSS tokens in globals.css verified for dual-theme WCAG AA contrast (both Light + Dark themes) |
| SC-009 | i18n namespace keys validated; keys present for all Shell strings; i18nProvider wraps MainTemplate |
