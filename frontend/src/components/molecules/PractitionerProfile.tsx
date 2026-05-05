/**
 * T036 [US2] PractitionerProfile molecule.
 *
 * Displays the practitioner's name, role, and optional avatar in the shell header.
 * This is a purely presentational ("dumb") component — all data is passed as props.
 *
 * Accessibility contract (required for T034 HeaderA11y tests):
 *   - root has aria-label="<name>, <role>"
 *   - avatar img (if present) has alt=<name>
 *   - data-testid="header-practitioner-profile" for test targeting
 *
 * Refs: FR-005, NFR-003
 */

import Avatar from "@mui/material/Avatar";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";

export interface PractitionerProfileProps {
  name: string;
  role: string;
  avatarUrl?: string;
}

export function PractitionerProfile({ name, role, avatarUrl }: PractitionerProfileProps) {
  return (
    <Box
      data-testid="header-practitioner-profile"
      aria-label={`${name}, ${role}`}
      sx={{ display: "flex", alignItems: "center", gap: 1 }}
    >
      <Avatar
        src={avatarUrl}
        alt={name}
        sx={{ width: 32, height: 32, fontSize: "0.85rem" }}
      >
        {!avatarUrl && name.charAt(0).toUpperCase()}
      </Avatar>
      <Box sx={{ display: "flex", flexDirection: "column", lineHeight: 1.2 }}>
        <Typography
          data-testid="header-practitioner-name"
          variant="body2"
          component="span"
          sx={{ fontWeight: 500, lineHeight: 1.3 }}
        >
          {name}
        </Typography>
        <Typography
          data-testid="header-practitioner-role"
          variant="caption"
          component="span"
          sx={{ color: '#ffffff', lineHeight: 1.2 }}
        >
          {role}
        </Typography>
      </Box>
    </Box>
  );
}
