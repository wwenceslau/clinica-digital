export const sidebarNamespaceKeys = {
  domains: [
    "sidebar.domain.administration",
    "sidebar.domain.professionals",
    "sidebar.domain.patients",
    "sidebar.domain.scheduling",
    "sidebar.domain.clinical-care",
    "sidebar.domain.diagnostics-therapy",
    "sidebar.domain.prevention",
    "sidebar.domain.billing",
    "sidebar.domain.security",
  ],
  administration: ["sidebar.admin.tenant-settings"],
  security: [
    "sidebar.security.user-management",
    "sidebar.security.access-profiles",
    "sidebar.security.audit",
  ],
} as const;

export const headerNamespaceKeys = [
  "header.tenant",
  "header.location",
  "header.profile",
  "header.select-unit",
] as const;

export const telemetryNamespaceKeys = ["telemetry.trace-id", "telemetry.tenant-id", "telemetry.debug-mode"] as const;

export const a11yNamespaceKeys = ["a11y.permission-restricted", "a11y.menu.open", "a11y.menu.close"] as const;

/** US7: Error message i18n keys for RNDS/IAM errors. */
export const errorNamespaceKeys = [
  "error.rnds.unsupported-profile",
  "error.rnds.structure-violation",
  "error.rnds.throttled",
  "error.rnds.cnes-invalid",
  "error.rnds.cnes-already-registered",
  "error.iam.invalid-credentials",
  "error.iam.account-locked",
  "error.iam.no-organizations",
  "error.iam.session-expired",
  "error.iam.forbidden",
  "error.network.connection",
  "error.generic",
] as const;

export const shellNamespaceKeys = [
  ...sidebarNamespaceKeys.domains,
  ...sidebarNamespaceKeys.administration,
  ...sidebarNamespaceKeys.security,
  ...headerNamespaceKeys,
  ...telemetryNamespaceKeys,
  ...a11yNamespaceKeys,
] as const;
