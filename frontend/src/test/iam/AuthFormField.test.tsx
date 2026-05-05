/**
 * T113 [US10] Unit tests for AuthFormField molecule.
 *
 * Verifies:
 * 1. Renders label and input with htmlFor/id linkage.
 * 2. Renders with type="email" for email variant.
 * 3. Renders with type="password" for password variant.
 * 4. Forwards value and onChange to the underlying input.
 * 5. Shows required attribute when required=true.
 * 6. Shows autoComplete attribute when provided.
 *
 * Refs: FR-013, US10
 */

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect } from 'vitest';
import { AuthFormField } from '../../components/molecules/AuthFormField';

describe('AuthFormField', () => {
  it('renders label linked to the input by id', () => {
    render(
      <AuthFormField
        id="auth-email"
        label="E-mail"
        type="email"
        value=""
        onChange={vi.fn()}
      />
    );
    expect(screen.getByLabelText(/e-?mail/i)).toBeInTheDocument();
  });

  it('renders email input with type="email"', () => {
    render(
      <AuthFormField
        id="auth-email"
        label="E-mail"
        type="email"
        value=""
        onChange={vi.fn()}
      />
    );
    expect(screen.getByLabelText(/e-?mail/i)).toHaveAttribute('type', 'email');
  });

  it('renders password input with type="password"', () => {
    render(
      <AuthFormField
        id="auth-password"
        label="Senha"
        type="password"
        value=""
        onChange={vi.fn()}
      />
    );
    expect(screen.getByLabelText(/senha/i)).toHaveAttribute('type', 'password');
  });

  it('forwards value to the underlying input', () => {
    render(
      <AuthFormField
        id="auth-email"
        label="E-mail"
        type="email"
        value="user@test.com"
        onChange={vi.fn()}
      />
    );
    const input = screen.getByLabelText(/e-?mail/i) as HTMLInputElement;
    expect(input.value).toBe('user@test.com');
  });

  it('calls onChange when the user types', async () => {
    const onChange = vi.fn();
    render(
      <AuthFormField
        id="auth-email"
        label="E-mail"
        type="email"
        value=""
        onChange={onChange}
      />
    );
    await userEvent.type(screen.getByLabelText(/e-?mail/i), 'a');
    expect(onChange).toHaveBeenCalled();
  });

  it('sets required attribute when required=true', () => {
    render(
      <AuthFormField
        id="auth-email"
        label="E-mail"
        type="email"
        value=""
        onChange={vi.fn()}
        required
      />
    );
    expect(screen.getByLabelText(/e-?mail/i)).toBeRequired();
  });

  it('sets autoComplete attribute when provided', () => {
    render(
      <AuthFormField
        id="auth-email"
        label="E-mail"
        type="email"
        value=""
        onChange={vi.fn()}
        autoComplete="email"
      />
    );
    expect(screen.getByLabelText(/e-?mail/i)).toHaveAttribute('autocomplete', 'email');
  });
});
