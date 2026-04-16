# Component Contract: Telemetry (Footer)

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-04-10  
**Component**: `ShellFooter` (molecule)  
**Location**: `frontend/src/components/molecules/ShellFooter.tsx`

## Purpose

Display observability metadata (trace_id, tenant_id) in a fixed footer, visible only when debug mode is active or user has support role.

## Props Interface

```typescript
interface ShellFooterProps {
  trace_id: string;
  tenant_id: string;
  user_id: string;
  visible: boolean;    // Calculated from debug_flag || user.role.includes('support')
}
```

## Visibility Rules

- **Visible** (render footer): `visible=true` (debug mode or support role)
- **Hidden** (CSS display:none): `visible=false` (production, non-support users)
- **Always Available**: Rendered as DOM attributes `data-trace-id` and `data-tenant-id` on MainTemplate root regardless of visibility

## Rendering

```html
<!-- If visible=true -->
<footer data-testid="shell-footer" style={{position: 'fixed', bottom: 0, width: '100%' }}>
  <div>Trace ID: {trace_id}</div>
  <div>Tenant ID: {tenant_id}</div>
</footer>

<!-- If visible=false -->
<footer data-testid="shell-footer" style={{display: 'none'}}>
  <!-- Same content, not rendered -->
</footer>

<!-- Always present as attributes -->
<div data-trace-id="{trace_id}" data-tenant-id="{tenant_id}">
  <!-- MainTemplate root -->
</div>
```

## Test Contract

- ✓ Renders footer when visible=true
- ✓ Hides footer when visible=false
- ✓ trace_id and tenant_id always present as data-* attributes
- ✓ Text content is selectable (copy-paste for debugging)
- ✓ WCAG 2.1 AA: sufficient contrast if visible, does not interfere with main tab order

## Success Criteria

- SC-004: trace_id and tenant_id exposed as data-* attributes
- SC-004: In debug/support mode, values visible in footer
- FR-007: Telemetry metadata accessible for diagnosis and audit
