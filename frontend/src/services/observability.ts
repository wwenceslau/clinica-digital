type ShellEventName =
  | "shell.render"
  | "shell.nav.click"
  | "shell.location.changed"
  | "shell.theme.changed"
  | "a11y.violation";

type ShellTelemetryEvent = {
  event: ShellEventName;
  trace_id: string;
  tenant_id: string;
  timestamp: string;
  details?: Record<string, string | number | boolean | null | undefined>;
};

function toPayload(
  event: ShellEventName,
  traceId: string,
  tenantId: string,
  details?: ShellTelemetryEvent["details"],
): ShellTelemetryEvent {
  return {
    event,
    trace_id: traceId,
    tenant_id: tenantId,
    timestamp: new Date().toISOString(),
    details,
  };
}

export function emitShellTelemetry(
  event: ShellEventName,
  traceId: string,
  tenantId: string,
  details?: ShellTelemetryEvent["details"],
): void {
  const payload = toPayload(event, traceId, tenantId, details);
  // Structured client log; backend shipping is integrated later.
  console.info("[shell-telemetry]", payload);
}

export function getDomTelemetryAttributes(traceId: string, tenantId: string): Record<string, string> {
  return {
    "data-trace-id": traceId,
    "data-tenant-id": tenantId,
  };
}
