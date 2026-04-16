# Component Contract: Sidebar

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-04-10  
**Component**: `Sidebar` (organism)  
**Location**: `frontend/src/components/organisms/Sidebar.tsx`

## Purpose

Hierarchical domain-based navigation menu for the authenticated area. Displays 9 functional domains, each containing multiple resources.

## Props Interface

```typescript
interface SidebarProps {
  domains: SidebarDomainGroup[];
  onItemClick: (resource_id: string, route: string) => void;
}

interface SidebarDomainGroup {
  domain_id: string;
  domain_label_key: string;
  resources: SidebarResourceItem[];
}

interface SidebarResourceItem {
  resource_id: string;
  label_key: string;
  icon_id: string;
  route: string;
  permitted: boolean;
  disabled_reason?: string;
}
```

## Key Interactions

1. **Domain Expand/Collapse**: Arrow Right/Left keys or click to toggle
2. **Resource Navigation**: Click or Enter key to navigate
3. **Permission Visibility**: Disabled items show tooltip explaining restriction

## Test Contract

- ✓ Renders all 9 domains and nested resources
- ✓ Expand/collapse works via keyboard (arrow keys) and mouse
- ✓ Navigation click calls onItemClick with resource_id and route
- ✓ Disabled items show tooltip and are keyboard-skippable
- ✓ WCAG 2.1 AA: full keyboard nav, contrast, focus visible
- ✓ Responsive: sidebar collapses/hides on mobile (future iteration)

## Success Criteria

- SC-001: 100% of domains/resources visible per Resource Table
- SC-002: Navigation find-time testable (developers locate domain/item < 10s)
- FR-002, FR-003: Domain + resource structure rendered correctly
- FR-011: Disabled items display with reduced opacity + tooltip
- FR-013: Full keyboard navigation + accessible contrast
