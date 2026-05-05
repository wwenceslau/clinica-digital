/**
 * ShellFooter molecule — debug overlay rendered only when debugMode=true.
 *
 * Renders telemetry attributes (data-trace-id, data-tenant-id) on its root
 * element via getDomTelemetryAttributes for observability tooling.
 *
 * When debugMode is false, the component returns null (nothing rendered to DOM).
 *
 * Usage:
 *   <ShellFooter traceId={traceId} tenantId={tenantId} debugMode={isDev} />
 *
 * Refs: FR-007, SC-004 / T046
 */

import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import { getDomTelemetryAttributes } from "../../services/observability";

export interface ShellFooterProps {
  traceId: string;
  tenantId: string;
  debugMode: boolean;
}

export function ShellFooter({ traceId, tenantId, debugMode }: ShellFooterProps) {
  if (!debugMode) return null;

  const telemetryAttrs = getDomTelemetryAttributes(traceId, tenantId);

  return (
    <Box
      component="footer"
      data-testid="shell-footer"
      {...telemetryAttrs}
      sx={{
        position: "fixed",
        bottom: 0,
        left: 0,
        right: 0,
        py: 0.5,
        px: 2,
        bgcolor: "rgba(0,0,0,0.75)",
        color: "#90caf9",
        fontSize: "0.7rem",
        fontFamily: "monospace",
        zIndex: (theme) => theme.zIndex.tooltip + 1,
        display: "flex",
        gap: 2,
      }}
    >
      <Typography component="span" variant="caption" fontFamily="monospace">
        trace: {traceId}
      </Typography>
      <Typography component="span" variant="caption" fontFamily="monospace">
        tenant: {tenantId}
      </Typography>
    </Box>
  );
}
