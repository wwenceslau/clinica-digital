/**
 * T047 [US3] OperationOutcomeAlert molecule.
 *
 * Renders a MUI Alert with the diagnostics from a FHIR OperationOutcome.
 * Used for error feedback in registration, login and other IAM flows.
 *
 * Refs: FR-009
 */

import Alert from '@mui/material/Alert';
import type { OperationOutcome } from '../../services/clinicRegistrationApi';

interface OperationOutcomeAlertProps {
  outcome: OperationOutcome | null;
}

export function OperationOutcomeAlert({ outcome }: OperationOutcomeAlertProps) {
  if (!outcome || !outcome.issue?.length) return null;

  const message = outcome.issue
    .map((issue) => issue.diagnostics ?? issue.code)
    .join(' • ');

  return (
    <Alert severity="error" role="alert" sx={{ mt: 2 }}>
      {message}
    </Alert>
  );
}
