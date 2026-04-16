# Accessibility Contract: Shell WCAG 2.1 AA Compliance

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-04-10  
**Target**: WCAG 2.1 Level AA  
**Spec Reference**: FR-013, SC-006

## Overview

This contract defines the accessibility requirements and validation strategies for all Shell components to ensure compliance with WCAG 2.1 Level AA standard.

## Core Principles

1. **Perceivable**: Content must be perceivable (text alternatives, adaptable, distinguishable)
2. **Operable**: Components must be fully keyboard-navigable and predictable
3. **Understandable**: Content must be readable and components must be predictable
4. **Robust**: Implementations must be compatible with assistive technologies (screen readers, etc.)

## Component-Level Requirements

### Header Component

**Perceivable**:
- Tenant name displayed with sufficient contrast (4.5:1 minimum ratio)
- Define color token `--shell-color-text` at 4.5:1 against background in both Light and Dark themes
- All icons accompanied by text labels or ARIA labels
- No color-only encoding (e.g., red for error requires text/symbol in addition)

**Operable**:
- Location dropdown fully keyboard-accessible (Tab to open, arrow keys to navigate, Enter to select, Escape to close)
- Practitioner profile clickable via Enter key on keyboard focus
- Minimum touch target size: 44×44px (CSS: `min-width: 44px; min-height: 44px;`)
- Focus indicators visible: at least 2px stroke width, high contrast against background

**Understandable**:
- Text labels clear and concise (e.g., "Select Unit" not "Unit")
- ARIA labels for screen readers: `aria-label="Clinic Name"`, `aria-label="Select Unit"`, `aria-label="User Profile"`
- Status messages for location changes (e.g., "Unit changed to Unidade 01")

**Robust**:
- Semantic HTML: `<header>`, `<h1>` for tenant name, `<nav>` not recommended here (nav is for Sidebar)
- ARIA: `role="banner"` on header element, `aria-current="page"` on active location
- No outdated HTML patterns (e.g., no `<div onclick>` without proper ARIA)

### Sidebar Component

**Perceivable**:
- All navigation items have text labels (not just icons)
- Disabled items (permission-restricted) have visual indication: reduced opacity (0.5) + tooltip explanation
- Contrast ratio for disabled text: 3:1 (relaxed WCAG requirement for disabled elements)
- Domain group headers clearly distinguish from resource items (font weight, size, or visual hierarchy)

**Operable**:
- Full keyboard navigation:
  - Tab: focus next item
  - Shift+Tab: focus previous item
  - Arrow Up/Down: navigate within expanded domain group
  - Arrow Right: expand collapsed domain group
  - Arrow Left: collapse expanded domain group
  - Enter/Space: activate navigation item
  - Escape: close any tooltips
- Focus order logical: top to bottom, left to right
- Touch targets: 44×44px minimum for all interactive elements
- Focus indicators: `outline: 2px solid {focus-color}` with sufficient contrast

**Understandable**:
- Domain group labels describe their purpose clearly (e.g., "Segurança" not "Section 9")
- Tooltips explain disabled reason clearly (e.g., "Restrito a administradores" not "N/A")
- Current page indicator: `aria-current="page"` on active resource item
- Breadcrumb or context trail (e.g., "Seg. > Usuários" in page title) helps users understand location

**Robust**:
- Semantic structure: `<nav role="navigation">` with `<ul>` lists
- ARIA attributes:
  - `aria-label="Navigation"` on `<nav>`
  - `aria-expanded="true"` / `"false"` on collapsible domain groups
  - `aria-disabled="true"` on disabled resource items (rendered as disabled button, not link)
  - `aria-describedby="tooltip-id"` linking to tooltip explanation
- Tree structure conveyed via semantic HTML and ARIA roles

### Footer (Telemetry) Component

**Perceivable**:
- Text labels for trace_id and tenant_id (not just values)
- Sufficient contrast if visible: 4.5:1 against footer background
- Small text permitted (≥ 12px) for debug information

**Operable**:
- Selectable text (copy-paste friendly for debugging)
- Not interrupting main navigation (not a modal or overlay)
- Removable/hideable per user preference (if debug toggle implemented in future iteration)

**Understandable**:
- Clear labeling: "Trace ID: {value}", "Tenant ID: {value}"
- Context that this is debug information

**Robust**:
- Semantic structure: `<footer>` element with `<dl>` (definition list) or `<p>` tags
- Not relying on JavaScript for content visibility (CSS `display: none` only)

## CSS Token Validation

Both Light and Dark themes must meet WCAG AA contrast requirements:

```css
/* Light theme - validate contrast ratios */
:root[data-theme="light"] {
  --shell-color-text: #000;                /* 21:1 against white background ✓ */
  --shell-color-text-secondary: #666;      /* 6.3:1 against white background ✓ */
  --shell-color-disabled: rgba(0,0,0,0.5); /* 10:1 against white (disabled text relaxed) ✓ */
  --shell-color-focus: #1976d2;            /* Blue focus indicator ✓ */
}

/* Dark theme - validate contrast ratios */
:root[data-theme="dark"] {
  --shell-color-text: #fff;                /* 21:1 against black background ✓ */
  --shell-color-text-secondary: #bbb;      /* 6:1 against dark background ✓ */
  --shell-color-disabled: rgba(255,255,255,0.5); /* 10:1 disabled (relaxed) ✓ */
  --shell-color-focus: #90caf9;            /* Light blue focus indicator ✓ */
}
```

**Validation Tool**: WebAIM Contrast Checker (https://webaim.org/resources/contrastchecker/)

## Testing & Validation Strategy

### Automated Checks

1. **axe-core** (npm package `@axe-core/react`):
   - Detects WCAG violations (colors, contrast, ARIA)
   - Run in unit tests: `cy.checkA11y()` or `await page.evaluate(() => axe.run())`
   - Target: Zero violations at AA level

2. **Lighthouse** (Chrome DevTools):
   - Accessibility score: ≥ 90/100
   - Reports missing alt text, low contrast, etc.
   - Run in CI/CD on every build

3. **WAVE** (WebAIM):
   - Browser extension for manual review
   - Checks: structure, ARIA usage, color contrast

### Manual Testing

1. **Keyboard Navigation**:
   - Tab through entire Shell
   - Verify focus visible on every interactive element
   - Test domain expand/collapse with arrow keys
   - Verify Tab order logical

2. **Screen Reader Testing**:
   - NVDA (Windows), JAWS (Windows), VoiceOver (macOS)
   - Verify: domain labels read clearly, disabled state announced, current page indicated
   - Test tooltips/descriptions announced

3. **Color Contrast**:
   - Use Contrast Checker on every color token
   - Test both Light and Dark themes
   - Verify 4.5:1 minimum for all text

4. **Zoom Testing**:
   - Zoom to 200% in browser
   - Verify no layout breakage, all text readable, no horizontal scrolling

### Testing Checklist

- [ ] axe-core automated audit passes (zero AA violations)
- [ ] Lighthouse accessibility ≥ 90/100
- [ ] Full keyboard navigation works (Tab, arrow keys, Enter, Escape)
- [ ] Focus indicators visible on all interactive elements (2px+ stroke)
- [ ] Screen reader announces domain names, resource labels, disabled state
- [ ] Current page item marked with `aria-current="page"`
- [ ] All color tokens meet 4.5:1 contrast (both themes)
- [ ] Touch targets ≥ 44×44px for mobile (if applicable)
- [ ] At 200% zoom, no layout breakage or horizontal scrolling
- [ ] Links distinguishable from text (underline or color + icon)
- [ ] Form fields have associated labels (not placeholder-only)
- [ ] Tooltips/help text associated via `aria-describedby`

## Evidence & Documentation

**A11y Audit Report** (`a11y-audit-report.md`):
- Date of audit
- Tool(s) used (axe-core, Lighthouse, manual)
- Results summary (violations, warnings, passes)
- Screenshots/logs for any deviations
- Sign-off by accessibility reviewer

**Test Results** (in `frontend/test/`):
- Unit test results from axe-core integration
- Playwright e2e a11y test results
- Screenshots of focus indicators

**CI/CD Integration**:
- Lighthouse audit runs on every PR
- axe-core checks run as part of unit test suite
- Merge blocked if A11y score < 90 or violations detected

## WCAG 2.1 Level AA Criteria Coverage

| Guideline | Success Criterion | Shell Component(s) | Status |
|---|---|---|---|
| 1.4.3 | Contrast (Minimum) — 4.5:1 for text | Colors in globals.css | Design |
| 2.1.1 | Keyboard — All functionality available via keyboard | Header, Sidebar | Impl |
| 2.1.2 | No Keyboard Trap — Focus can move away using keyboard alone | All interactive elements | Impl |
| 2.4.3 | Focus Order — Logical and meaningful order | Header → Sidebar → MainContent | Impl |
| 2.4.7 | Focus Visible — Keyboard focus indicator visible | All interactive elements | Design |
| 3.2.4 | Consistent Identification — Components behave consistently | Buttons, links, selectors | Impl |
| 4.1.2 | Name, Role, Value — Proper semantics for screen readers | ARIA labels, roles | Impl |
| 4.1.3 | Status Messages — Changes announced to screen readers | Location change, toast notifications | Impl |

## Related Contracts

- [MainTemplate Component Contract](main-template-contract.md)
- [Header Component Contract](header-contract.md)
- [Sidebar Component Contract](sidebar-contract.md)
