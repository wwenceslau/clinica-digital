# Component Contract: MainTemplate

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-04-10  
**Component**: `MainTemplate` (template organism)  
**Location**: `frontend/src/components/templates/MainTemplate.tsx`

## Purpose

Root layout container for the authenticated area. Wraps all authenticated routes and provides application-level context (theme, i18n, tenant/trace context).

## Props Interface

```typescript
interface MainTemplateProps {
  /**
   * Child routes/pages to render within the Shell layout
   */
  children: React.ReactNode;

  /**
   * Initial theme for the application
   * @default 'light'
   * @values 'light' | 'dark'
   */
  initialTheme?: 'light' | 'dark';

  /**
   * Initial locale for i18n
   * @default 'pt-BR'
   * @values 'pt-BR' | 'en-US'
   */
  initialLocale?: 'pt-BR' | 'en-US';

  /**
   * Tenant & request context passed from authentication layer
   */
  trainingContext: {
    /**
     * UUID of the clinic/tenant organization
     * @example 'clinic-0001'
     */
    tenant_id: string;

    /**
     * UUID for request tracing (from HTTP header or backend context)
     * @example 'trace-550e8400-e29b-41d4-a716-446655440000'
     */
    trace_id: string;

    /**
     * UUID of authenticated user/practitioner
     * @example 'user-550e8400'
     */
    user_id: string;

    /**
     * Array of clinical roles assigned to user
     * @example ['physician', 'admin']
     */
    role: string[];
  };
}
```

## Responsibilities

1. **Context Provision**:
   - Wrap children with `ThemeProvider` (MUI) configured for dual-theme (light/dark)
   - Wrap children with `I18nextProvider` to enable i18n
   - Wrap children with `ShellContext.Provider` to distribute tenant/trace/user context
   - Wrap children with `LocationContext.Provider` for active location state

2. **Layout Rendering**:
   - Render structural regions: Header (top), Sidebar (left), MainContent (center), Footer (bottom)
   - Header displays: tenant name, location selector, practitioner profile
   - Sidebar displays: hierarchical domain/resource navigation
   - MainContent: `{children}` (page content)
   - Footer: trace_id/tenant_id (conditional per debug mode/role)

3. **State Management**:
   - Track active theme state; persist to localStorage (`shell.theme.{user_id}`)
   - Track active locale state; persist to localStorage (`shell.locale.{user_id}`)
   - Track active location selection; persist to localStorage (`shell.active_location.{tenant_id}.{user_id}`)
   - Load navigation schema from backend on mount (via `useEffect`)

4. **Accessibility**:
   - Semantic HTML: `<header>`, `<nav>`, `<main>`, `<footer>` elements
   - ARIA landmarks: `role="banner"` (header), `role="navigation"` (sidebar), `role="main"` (content)
   - Focus management: keyboard navigation between main regions (Tab/Shift+Tab)
   - Visible focus indicators on all interactive elements (buttons, links, selectors)

## Exported API

```typescript
export default MainTemplate;
export { MainTemplateProps };
```

## Usage Example

```typescript
import MainTemplate from '../components/templates/MainTemplate';

<MainTemplate
  initialTheme="light"
  initialLocale="pt-BR"
  trainingContext={{
    tenant_id: 'clinic-0001',
    trace_id: 'trace-550e8400-e29b-41d4-a716-446655440000',
    user_id: 'user-550e8400',
    role: ['physician', 'admin'],
  }}
>
  <Routes>
    <Route path="/" element={<DashboardPage />} />
    <Route path="/patients/*" element={<PatientsPage />} />
  </Routes>
</MainTemplate>
```

## Internal Dependencies

- **MUI 7**: `ThemeProvider`, `Box`, `CssBaseline`
- **react-i18next**: `I18nextProvider`
- **React Context**: Custom `ShellContext`, `LocationContext`, `ThemeContext`
- **Child Components**:
  - `Header` (organism)
  - `Sidebar` (organism)
  - `ShellFooter` (molecule)

## Test Contract

**Unit Tests Required**:
1. ✓ Renders Header, Sidebar, MainContent, and Footer
2. ✓ Applies initial theme (light/dark) correctly
3. ✓ Applies initial locale correctly
4. ✓ Loads and renders navigation schema
5. ✓ Persists theme/locale/location to localStorage
6. ✓ Passes context to child components
7. ✓ Restores theme/locale/location from localStorage on remount
8. ✓ Handles missing/invalid navigation schema gracefully
9. ✓ Exposes trace_id/tenant_id as data-* attributes in DOM
10. ✓ Hides/shows telemetry footer per debug flag/role
11. ✓ Passes WCAG 2.1 AA accessibility audit (axe-core)

**E2E Tests Required**:
1. ✓ Navigation between domains visible and functional
2. ✓ Location change persists across page reload
3. ✓ Theme change persists across page reload
4. ✓ LCP ≤ 1.5s on 4G simulated network
5. ✓ Keyboard navigation works (Tab through Header → Sidebar → MainContent)

## Success Criteria (Spec Mapped)

- FR-001 ✓ MainTemplate wraps ThemeProvider + i18nProvider + custom contexts
- FR-006 ✓ Tenant isolation enforced in Header/Footer context display
- FR-007 ✓ trace_id/tenant_id exposed as data-* attributes and conditionally in footer
- FR-008, FR-015 ✓ Type attribute set to `data-theme="light"` or `"dark"` on root
- FR-013 ✓ Semantic HTML + ARIA landmarks + keyboard navigation + focus indicators
- FR-014, SC-007 ✓ LCP measured and documented in quickstart.md

## Related Contracts

- [Header Component Contract](header-contract.md)
- [Sidebar Component Contract](sidebar-contract.md)
- [Telemetry Contract](telemetry-contract.md)
