/**
 * T116 [US10] AuthFormField molecule.
 *
 * Reusable form field for authentication screens (login, registration).
 * Wraps MUI TextField with:
 *   - Explicit `id` prop for htmlFor/aria-label linkage (a11y).
 *   - Standardized autocomplete attribute forwarding.
 *   - Tailwind utility classes for consistent spacing.
 *
 * Used by LoginForm and ClinicRegistrationForm organisms.
 * Refs: FR-013
 */

import TextField from '@mui/material/TextField';

export interface AuthFormFieldProps {
  /** HTML id for the input — must be unique on the page. Used for accessible labelling. */
  id: string;
  /** Visible label text rendered by MUI InputLabel. */
  label: string;
  /** Input type: "email", "password", or "text". */
  type: 'email' | 'password' | 'text';
  /** Controlled value. */
  value: string;
  /** Change handler receives the new string value. */
  onChange: (value: string) => void;
  /** Whether the field is required (adds * to label, required attribute). */
  required?: boolean;
  /** HTML autocomplete attribute value. */
  autoComplete?: string;
  /** Whether the field is disabled. */
  disabled?: boolean;
  /** Optional helper/error text below the field. */
  helperText?: string;
  /** When true, field is in error state (red border). */
  error?: boolean;
  /** Optional data-testid override. Defaults to `auth-field-{id}`. */
  testId?: string;
}

export function AuthFormField({
  id,
  label,
  type,
  value,
  onChange,
  required = false,
  autoComplete,
  disabled = false,
  helperText,
  error = false,
  testId,
}: AuthFormFieldProps) {
  return (
    <TextField
      id={id}
      label={label}
      type={type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      required={required}
      fullWidth
      autoComplete={autoComplete}
      disabled={disabled}
      helperText={helperText}
      error={error}
      inputProps={{
        'aria-label': label,
        'data-testid': testId ?? `auth-field-${id}`,
      }}
      InputLabelProps={{ htmlFor: id }}
      className="w-full"
    />
  );
}
