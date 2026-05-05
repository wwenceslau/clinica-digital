/**
 * T130 — Frontend render performance test.
 *
 * Validates that the initial render of the Login component completes within
 * 1 500 ms on a simulated low-end device (4G profile).
 *
 * Strategy:
 * - Uses `performance.mark` + `performance.measure` (available in jsdom via
 *   the Web Performance API polyfill provided by Vitest's jsdom environment).
 * - Renders LoginForm in isolation and measures time-to-first-frame.
 * - Threshold: 1 500 ms total (generous for CI; real 4G simulation is
 *   handled by Playwright Lighthouse runs, which are run separately in the
 *   performance CI job).
 *
 * Refs: SC-001 (frontend render < 1.5 s, perfil 4G simulado)
 */

import { render } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { LoginForm } from '../components/organisms/LoginForm';

vi.mock('../services/iamAuthApi', () => ({
  login: vi.fn(),
  selectOrganization: vi.fn(),
}));

describe('LoginForm render performance (T130)', () => {
  it('renders LoginForm within 1500 ms', () => {
    performance.mark('login-render-start');

    render(<LoginForm onLogin={vi.fn()} />);

    performance.mark('login-render-end');
    performance.measure('login-render', 'login-render-start', 'login-render-end');

    const entries = performance.getEntriesByName('login-render');
    expect(entries).toHaveLength(1);

    const durationMs = entries[0].duration;

    console.log(`[T130] LoginForm render duration: ${durationMs.toFixed(2)} ms (threshold: 1500 ms)`);

    expect(durationMs).toBeLessThan(1500);

    performance.clearMarks('login-render-start');
    performance.clearMarks('login-render-end');
    performance.clearMeasures('login-render');
  });
});
