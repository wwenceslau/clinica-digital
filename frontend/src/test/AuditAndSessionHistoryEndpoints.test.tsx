import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { SecurityAuditPage } from '../app/SecurityAuditPage';
import { SessionHistory } from '../components/organisms/SessionHistory';

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    session: {
      sessionId: 'session-163',
      tenant: { id: 'tenant-163' },
    },
  }),
}));

afterEach(() => {
  vi.restoreAllMocks();
});

describe('T163 frontend endpoint integration', () => {
  it('loads audit trail from /api/admin/audit with tenant/session headers', async () => {
    const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ([
        {
          id: 'evt-001',
          actorUserId: 'usr-001',
          eventType: 'iam.login',
          outcome: 'success',
          traceId: 'trace-001',
          createdAt: '2026-05-06T10:00:00Z',
        },
      ]),
    } as Response);

    render(<SecurityAuditPage />);

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith('/api/admin/audit?limit=50', {
        headers: {
          'X-Tenant-ID': 'tenant-163',
          Authorization: 'Bearer session-163',
        },
      });
    });

    expect(await screen.findByText('iam.login')).toBeInTheDocument();
    expect(screen.getByText('usr-001')).toBeInTheDocument();
  });

  it.fails('session history should be integrated with /api/admin/sessions endpoint (pending T174)', async () => {
    const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ([
        {
          id: 'sess-001',
          tenantSlug: 'tenant-163',
          user: 'user@clinic.local',
          outcome: 'success',
          traceId: 'trace-sess-001',
        },
      ]),
    } as Response);

    render(<SessionHistory sessions={[]} />);

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith('/api/admin/sessions', expect.anything());
    });
  });
});
