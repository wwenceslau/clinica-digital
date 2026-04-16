# Component Contract: Header

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-04-10  
**Component**: `Header` (organism)  
**Location**: `frontend/src/components/organisms/Header.tsx`

## Purpose

Display tenant context (clinic name), active location selector, and practitioner profile in the application header.

## Props Interface

```typescript
interface HeaderProps {
  tenant_name: string;                  // e.g., "Clínica Digital"
  available_locations: LocationOption[];
  active_location_id: string;
  active_location_name?: string;
  practitioner_name: string;
  practitioner_role: string;
  practitioner_avatar_url?: string;
  onLocationChange: (location_id: string) => void;
}

interface LocationOption {
  location_id: string;
  location_name: string;
}
```

## Key Interactions

1. **Tenant Display**: Non-editable, shows clinic name prominently
2. **Location Selector**: Dropdown menu with localStorage persistence
3. **Practitioner Profile**: Click to show profile menu (future iteration)

## Test Contract

- ✓ Renders tenant name, location selector, practitioner info
- ✓ Location change triggers onLocationChange callback
- ✓ WCAG 2.1 AA: keyboard accessible, 4.5:1 contrast, focus visible
- ✓ Responsive: adapts to mobile/tablet (future iteration)

## Success Criteria

- SC-003: Tenant, location, profile visible in 100% of authenticated screens
- FR-005: Header displays all required context
- FR-012: Location selection persists and calls backend confirmation
- FR-013: Full keyboard navigation + accessible color contrast
